package com.duolinfo.ia.proj.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.response.SystemLogCleanupResponseDTO;
import com.duolinfo.ia.proj.dto.response.SystemLogEntryDTO;
import com.duolinfo.ia.proj.dto.response.SystemLogFileContentResponseDTO;
import com.duolinfo.ia.proj.dto.response.SystemLogFileInfoDTO;
import com.duolinfo.ia.proj.dto.response.SystemLogFileListResponseDTO;

import jakarta.annotation.PostConstruct;

@Service
public class SystemLogFileService {

    private static final Logger log = LoggerFactory.getLogger(SystemLogFileService.class);

    private static final int MAX_CACHED_LOG_ENTRIES = 15_000;
    private static final int MAX_CONTINUATION_CHARS = 8_000;
    private static final Set<String> ACTIVE_STREAM_FILES = Set.of(
        "application.log",
        "errors.log",
        "security-audit.log"
    );

    private static final Pattern LOG_LINE_PATTERN = Pattern.compile(
        "^(\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2})?)\\s+"
            + "(TRACE|DEBUG|INFO|WARN|ERROR)\\s+"
            + "\\[(.*?)]\\s+"
            + "\\[(.*?)]\\s+"
            + "(\\S+)\\s*:\\s*(.*)$"
    );

    private static final Pattern FILENAME_DATE_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");

    private final Path baseDirectory;
    private final int retentionDays;

    @Autowired
    public SystemLogFileService(
            @Value("${app.logging.dir:./logs}") String logDir,
            @Value("${app.logging.retention-days:30}") int retentionDays) {
        this(Paths.get(logDir == null || logDir.isBlank() ? "./logs" : logDir), retentionDays);
    }

    public SystemLogFileService(Path baseDirectory, int retentionDays) {
        this.baseDirectory = baseDirectory.toAbsolutePath().normalize();
        this.retentionDays = Math.max(1, retentionDays);
    }

    @PostConstruct
    public void initializeAndClean() {
        ensureBaseDirectory();
        purgeExpiredLogs();
    }

    public SystemLogFileListResponseDTO listLogFiles(String channelFilter) {
        ensureBaseDirectory();
        if (!Files.exists(baseDirectory)) {
            return new SystemLogFileListResponseDTO(
                retentionDays,
                baseDirectory.toString(),
                0,
                0L,
                List.of()
            );
        }

        List<SystemLogFileInfoDTO> files = new ArrayList<>();
        try (Stream<Path> stream = Files.list(baseDirectory)) {
            stream
                .filter(path -> Files.isRegularFile(path) && !Files.isSymbolicLink(path))
                .filter(path -> isAllowedLogExtension(path.getFileName().toString()))
                .map(this::toFileInfo)
                .filter(info -> matchesChannel(info.channel(), channelFilter))
                .sorted(Comparator
                    .comparing(SystemLogFileInfoDTO::lastModified, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(SystemLogFileInfoDTO::fileName))
                .forEach(files::add);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao listar arquivos de log.", e);
        }

        long totalBytes = files.stream().mapToLong(SystemLogFileInfoDTO::sizeBytes).sum();
        return new SystemLogFileListResponseDTO(
            retentionDays,
            baseDirectory.toString(),
            files.size(),
            totalBytes,
            files
        );
    }

    public SystemLogFileContentResponseDTO readLogFile(
            String fileName,
            String severityFilter,
            String search,
            int page,
            int limit) {
        Path filePath = resolveSafeLogPath(fileName);
        SystemLogFileInfoDTO fileInfo = toFileInfo(filePath);

        int safePage = Math.max(0, page);
        int safeLimit = Math.max(1, Math.min(limit, 200));
        String normalizedSeverity = severityFilter != null ? severityFilter.trim().toUpperCase(Locale.ROOT) : "";
        String normalizedSearch = search != null ? search.trim().toLowerCase(Locale.ROOT) : "";

        ArrayDeque<MutableLogEntry> recentEntries = new ArrayDeque<>(Math.min(MAX_CACHED_LOG_ENTRIES, 512));
        long totalLogicalEntries = 0L;
        long infoCount = 0L;
        long warnCount = 0L;
        long errorCount = 0L;

        try (BufferedReader reader = openReader(filePath, fileInfo.compressed())) {
            String rawLine;
            long physicalLineNumber = 0L;
            MutableLogEntry current = null;

            while ((rawLine = reader.readLine()) != null) {
                physicalLineNumber++;
                Matcher matcher = LOG_LINE_PATTERN.matcher(rawLine);

                if (matcher.matches()) {
                    if (current != null) {
                        totalLogicalEntries++;
                        switch (current.level) {
                            case "ERROR" -> errorCount++;
                            case "WARN" -> warnCount++;
                            default -> infoCount++;
                        }
                        appendBounded(recentEntries, current);
                    }
                    current = new MutableLogEntry(
                        physicalLineNumber,
                        matcher.group(1),
                        matcher.group(2),
                        matcher.group(3).trim(),
                        matcher.group(4).trim(),
                        matcher.group(5).trim(),
                        matcher.group(6)
                    );
                } else if (current != null) {
                    current.appendContinuation(rawLine);
                } else {
                    String inferredLevel = inferSeverity(rawLine);
                    current = new MutableLogEntry(
                        physicalLineNumber,
                        "",
                        inferredLevel,
                        "-",
                        "NO_CORR_ID",
                        "system",
                        rawLine
                    );
                }
            }

            if (current != null) {
                totalLogicalEntries++;
                switch (current.level) {
                    case "ERROR" -> errorCount++;
                    case "WARN" -> warnCount++;
                    default -> infoCount++;
                }
                appendBounded(recentEntries, current);
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao ler o arquivo de log.", e);
        }

        List<SystemLogEntryDTO> ordered = new ArrayList<>(recentEntries.size());
        for (MutableLogEntry entry : recentEntries) {
            ordered.add(entry.toDto());
        }
        Collections.reverse(ordered);

        List<SystemLogEntryDTO> filtered = ordered.stream()
            .filter(entry -> normalizedSeverity.isEmpty() || "ALL".equals(normalizedSeverity)
                || entry.level().equalsIgnoreCase(normalizedSeverity))
            .filter(entry -> normalizedSearch.isEmpty()
                || entry.rawLine().toLowerCase(Locale.ROOT).contains(normalizedSearch)
                || entry.correlationId().toLowerCase(Locale.ROOT).contains(normalizedSearch)
                || entry.logger().toLowerCase(Locale.ROOT).contains(normalizedSearch))
            .toList();

        long matchedCount = filtered.size();
        int fromIndex = safePage * safeLimit;
        List<SystemLogEntryDTO> paged = fromIndex >= filtered.size()
            ? List.of()
            : filtered.subList(fromIndex, Math.min(filtered.size(), fromIndex + safeLimit));

        return new SystemLogFileContentResponseDTO(
            fileInfo.fileName(),
            fileInfo.channel(),
            fileInfo.sizeBytes(),
            fileInfo.lastModified(),
            retentionDays,
            totalLogicalEntries,
            matchedCount,
            infoCount,
            warnCount,
            errorCount,
            safePage,
            safeLimit,
            paged
        );
    }

    public Path resolveSafeLogPath(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome do arquivo de log é obrigatório.");
        }
        String trimmed = fileName.trim();
        if (trimmed.contains("/") || trimmed.contains("\\") || trimmed.contains("..") || trimmed.indexOf('\0') >= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome de arquivo inválido.");
        }
        if (!isAllowedLogExtension(trimmed)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de arquivo de log não permitido.");
        }

        Path candidate = baseDirectory.resolve(trimmed).normalize();
        if (!candidate.startsWith(baseDirectory) || !baseDirectory.equals(candidate.getParent())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Acesso fora do diretório de logs negado.");
        }
        if (Files.isSymbolicLink(candidate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Links simbólicos de log não são permitidos.");
        }
        if (!Files.exists(candidate) || !Files.isRegularFile(candidate)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo de log não encontrado.");
        }
        return candidate;
    }

    @Scheduled(cron = "0 15 3 * * *")
    public SystemLogCleanupResponseDTO purgeExpiredLogs() {
        if (!Files.exists(baseDirectory)) {
            return new SystemLogCleanupResponseDTO(0, 0L, retentionDays, Instant.now());
        }

        Instant cutoffInstant = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        LocalDate cutoffDate = LocalDate.now().minusDays(retentionDays);
        int deleted = 0;
        long freedBytes = 0L;

        try (Stream<Path> stream = Files.list(baseDirectory)) {
            List<Path> candidates = stream
                .filter(path -> Files.isRegularFile(path) && !Files.isSymbolicLink(path))
                .filter(path -> isAllowedLogExtension(path.getFileName().toString()))
                .toList();

            for (Path candidate : candidates) {
                String name = candidate.getFileName().toString();
                if (ACTIVE_STREAM_FILES.contains(name)) {
                    continue;
                }

                try {
                    Instant modified = Files.getLastModifiedTime(candidate).toInstant();
                    LocalDate fileDate = parseDateFromFilename(name);
                    boolean expiredByTimestamp = modified.isBefore(cutoffInstant);
                    boolean expiredByFilenameDate = fileDate != null && fileDate.isBefore(cutoffDate);

                    if (expiredByTimestamp || expiredByFilenameDate) {
                        long size = Files.size(candidate);
                        if (Files.deleteIfExists(candidate)) {
                            deleted++;
                            freedBytes += size;
                        }
                    }
                } catch (IOException ex) {
                    log.warn("[LOG-RETENTION] Não foi possível remover arquivo expirado {}: {}", name, ex.getMessage());
                }
            }
        } catch (IOException e) {
            log.warn("[LOG-RETENTION] Erro ao varrer diretório de logs {}: {}", baseDirectory, e.getMessage());
        }

        if (deleted > 0) {
            log.info("[LOG-RETENTION] Removidos {} arquivo(s) de log com mais de {} dias ({} bytes liberados).",
                deleted, retentionDays, freedBytes);
        }
        return new SystemLogCleanupResponseDTO(deleted, freedBytes, retentionDays, Instant.now());
    }

    private void ensureBaseDirectory() {
        try {
            if (!Files.exists(baseDirectory)) {
                Files.createDirectories(baseDirectory);
            }
        } catch (IOException e) {
            log.warn("[LOG-DIR] Falha ao garantir diretório de logs {}: {}", baseDirectory, e.getMessage());
        }
    }

    private SystemLogFileInfoDTO toFileInfo(Path path) {
        String name = path.getFileName().toString();
        String channel = detectChannel(name);
        String label = channelLabel(channel);
        boolean compressed = name.toLowerCase(Locale.ROOT).endsWith(".gz");
        long size = 0L;
        Instant lastModified = Instant.now();
        try {
            size = Files.size(path);
            lastModified = Files.getLastModifiedTime(path).toInstant();
        } catch (IOException ignored) {
            // Keep safe defaults
        }

        LocalDate fileDate = parseDateFromFilename(name);
        String dateText = fileDate != null
            ? fileDate.toString()
            : lastModified.atZone(ZoneId.systemDefault()).toLocalDate().toString();

        return new SystemLogFileInfoDTO(name, channel, label, dateText, compressed, size, lastModified);
    }

    private static BufferedReader openReader(Path filePath, boolean compressed) throws IOException {
        InputStream raw = Files.newInputStream(filePath);
        InputStream stream = compressed ? new GZIPInputStream(raw) : raw;
        var decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE);
        return new BufferedReader(new InputStreamReader(stream, decoder));
    }

    private static void appendBounded(ArrayDeque<MutableLogEntry> deque, MutableLogEntry entry) {
        if (deque.size() >= MAX_CACHED_LOG_ENTRIES) {
            deque.pollFirst();
        }
        deque.addLast(entry);
    }

    private static boolean isAllowedLogExtension(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".log") || lower.endsWith(".log.gz");
    }

    private static boolean matchesChannel(String channel, String channelFilter) {
        if (channelFilter == null || channelFilter.isBlank() || "ALL".equalsIgnoreCase(channelFilter)) {
            return true;
        }
        return channel.equalsIgnoreCase(channelFilter.trim());
    }

    private static String detectChannel(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.startsWith("errors")) {
            return "errors";
        }
        if (lower.startsWith("security-audit")) {
            return "security-audit";
        }
        if (lower.startsWith("application")) {
            return "application";
        }
        return "other";
    }

    private static String channelLabel(String channel) {
        return switch (channel) {
            case "errors" -> "Erros & Alertas";
            case "security-audit" -> "Segurança & Auditoria";
            case "application" -> "Aplicação Geral";
            default -> "Sistema";
        };
    }

    private static LocalDate parseDateFromFilename(String name) {
        Matcher matcher = FILENAME_DATE_PATTERN.matcher(name);
        if (!matcher.find()) {
            return null;
        }
        try {
            return LocalDate.parse(matcher.group(1));
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static String inferSeverity(String rawLine) {
        String upper = rawLine.toUpperCase(Locale.ROOT);
        if (upper.contains(" ERROR ") || upper.startsWith("ERROR")) {
            return "ERROR";
        }
        if (upper.contains(" WARN ") || upper.startsWith("WARN")) {
            return "WARN";
        }
        return "INFO";
    }

    private static final class MutableLogEntry {
        final long lineNumber;
        final String timestamp;
        final String level;
        final String thread;
        final String correlationId;
        final String logger;
        final StringBuilder message;
        final StringBuilder raw;

        MutableLogEntry(
                long lineNumber,
                String timestamp,
                String level,
                String thread,
                String correlationId,
                String logger,
                String firstMessage) {
            this.lineNumber = lineNumber;
            this.timestamp = timestamp;
            this.level = level;
            this.thread = thread;
            this.correlationId = correlationId.isEmpty() ? "NO_CORR_ID" : correlationId;
            this.logger = logger;
            this.message = new StringBuilder(firstMessage);
            this.raw = new StringBuilder(
                timestamp.isEmpty()
                    ? firstMessage
                    : timestamp + " " + level + " [" + thread + "] [" + this.correlationId + "] " + logger + " : " + firstMessage
            );
        }

        void appendContinuation(String line) {
            if (message.length() < MAX_CONTINUATION_CHARS) {
                message.append('\n').append(line);
                raw.append('\n').append(line);
            }
        }

        SystemLogEntryDTO toDto() {
            return new SystemLogEntryDTO(
                lineNumber,
                timestamp,
                level,
                thread,
                correlationId,
                logger,
                message.toString(),
                raw.toString()
            );
        }
    }
}
