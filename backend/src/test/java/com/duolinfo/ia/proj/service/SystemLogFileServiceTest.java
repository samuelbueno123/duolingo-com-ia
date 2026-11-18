package com.duolinfo.ia.proj.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.response.SystemLogCleanupResponseDTO;
import com.duolinfo.ia.proj.dto.response.SystemLogFileContentResponseDTO;
import com.duolinfo.ia.proj.dto.response.SystemLogFileListResponseDTO;

class SystemLogFileServiceTest {

    @TempDir
    Path tempLogDir;

    @Test
    @DisplayName("Separa canais, lê arquivos .log e .log.gz, agrupa stacktraces e filtra por severidade e correlationId")
    void shouldListReadParseAndFilterPlainAndGzipLogs() throws IOException {
        Path appFile = tempLogDir.resolve("application.log");
        Files.writeString(appFile, String.join("\n",
            "2026-11-18T10:00:00.123-03:00 INFO  [           main] [corr-001] c.d.i.p.ProjApplication                  : Inicialização concluída",
            "2026-11-18T10:01:00.456-03:00 WARN  [   http-nio-8080] [corr-002] c.d.i.p.service.AuthService              : Tentativa com token expirado",
            "2026-11-18T10:02:00.789-03:00 ERROR [   http-nio-8080] [corr-003] c.d.i.p.GlobalExceptionHandler           : Falha inesperada",
            "java.lang.IllegalStateException: Erro simulado",
            "\tat com.duolinfo.ia.proj.Demo.run(Demo.java:42)"
        ), StandardCharsets.UTF_8);

        Path errArchive = tempLogDir.resolve("errors-2026-11-17.0.log.gz");
        byte[] gzContent = gzip(String.join("\n",
            "2026-11-17T22:10:00.000-03:00 ERROR [       worker-1] [corr-999] c.d.i.p.Job                              : Falha arquivada em gzip"
        ));
        Files.write(errArchive, gzContent);

        Path auditFile = tempLogDir.resolve("security-audit.log");
        Files.writeString(auditFile,
            "2026-11-18T10:03:00.000-03:00 INFO  [   http-nio-8080] [corr-004] c.d.i.p.SECURITY_AUDIT                   : [AUDIT-RECORDED] action=LOGIN_SUCCESS",
            StandardCharsets.UTF_8);

        SystemLogFileService service = new SystemLogFileService(tempLogDir, 30);

        SystemLogFileListResponseDTO listed = service.listLogFiles(null);
        assertEquals(30, listed.retentionDays());
        assertEquals(3, listed.totalFiles());
        assertTrue(listed.files().stream().anyMatch(f -> f.fileName().equals("application.log") && f.channel().equals("application")));
        assertTrue(listed.files().stream().anyMatch(f -> f.fileName().equals("errors-2026-11-17.0.log.gz") && f.compressed() && f.channel().equals("errors")));
        assertTrue(listed.files().stream().anyMatch(f -> f.fileName().equals("security-audit.log") && f.channel().equals("security-audit")));

        // Filtra canal errors
        SystemLogFileListResponseDTO errorsOnly = service.listLogFiles("errors");
        assertEquals(1, errorsOnly.totalFiles());

        // Lê application.log: ordem reversa (mais novo primeiro) + stacktrace agrupada no ERROR
        SystemLogFileContentResponseDTO appContent = service.readLogFile("application.log", null, null, 0, 50);
        assertEquals(3, appContent.totalLines());
        assertEquals(1, appContent.infoCount());
        assertEquals(1, appContent.warnCount());
        assertEquals(1, appContent.errorCount());
        assertEquals("ERROR", appContent.entries().get(0).level());
        assertTrue(appContent.entries().get(0).message().contains("java.lang.IllegalStateException"));

        // Filtra por correlationId e severidade
        SystemLogFileContentResponseDTO filtered = service.readLogFile("application.log", "WARN", "corr-002", 0, 50);
        assertEquals(1, filtered.matchedLines());
        assertEquals("WARN", filtered.entries().get(0).level());

        // Lê arquivo compactado .log.gz
        SystemLogFileContentResponseDTO gzRead = service.readLogFile("errors-2026-11-17.0.log.gz", null, null, 0, 50);
        assertEquals(1, gzRead.totalLines());
        assertTrue(gzRead.entries().get(0).message().contains("Falha arquivada em gzip"));
    }

    @Test
    @DisplayName("Bloqueia path traversal e extensões inválidas ao acessar arquivos de log")
    void shouldRejectPathTraversalAndInvalidExtensions() throws IOException {
        Files.writeString(tempLogDir.resolve("application.log"), "ok", StandardCharsets.UTF_8);
        SystemLogFileService service = new SystemLogFileService(tempLogDir, 30);

        ResponseStatusException traversal = assertThrows(ResponseStatusException.class,
            () -> service.resolveSafeLogPath("../application.properties"));
        assertEquals(HttpStatus.BAD_REQUEST, traversal.getStatusCode());

        ResponseStatusException slash = assertThrows(ResponseStatusException.class,
            () -> service.resolveSafeLogPath("sub/application.log"));
        assertEquals(HttpStatus.BAD_REQUEST, slash.getStatusCode());

        ResponseStatusException wrongExt = assertThrows(ResponseStatusException.class,
            () -> service.resolveSafeLogPath("secret.txt"));
        assertEquals(HttpStatus.BAD_REQUEST, wrongExt.getStatusCode());

        ResponseStatusException missing = assertThrows(ResponseStatusException.class,
            () -> service.resolveSafeLogPath("missing-2026-11-18.0.log.gz"));
        assertEquals(HttpStatus.NOT_FOUND, missing.getStatusCode());
    }

    @Test
    @DisplayName("Remove arquivos rotacionados com mais de 30 dias mantendo recentes e ativos")
    void shouldPurgeLogsOlderThan30DaysWhilePreservingRecentAndActiveFiles() throws IOException {
        Path activeApp = Files.writeString(tempLogDir.resolve("application.log"), "active", StandardCharsets.UTF_8);
        Files.setLastModifiedTime(activeApp, FileTime.from(Instant.now().minus(45, ChronoUnit.DAYS)));

        LocalDate tenDaysAgo = LocalDate.now().minusDays(10);
        Path recentArchive = Files.write(
            tempLogDir.resolve("application-" + tenDaysAgo + ".0.log.gz"),
            gzip("recent")
        );

        LocalDate fortyDaysAgo = LocalDate.now().minusDays(40);
        Path expiredByDate = Files.write(
            tempLogDir.resolve("errors-" + fortyDaysAgo + ".0.log.gz"),
            gzip("expired-by-date")
        );

        Path expiredByMtime = Files.writeString(tempLogDir.resolve("legacy-batch.log"), "old", StandardCharsets.UTF_8);
        Files.setLastModifiedTime(expiredByMtime, FileTime.from(Instant.now().minus(35, ChronoUnit.DAYS)));

        SystemLogFileService service = new SystemLogFileService(tempLogDir, 30);
        SystemLogCleanupResponseDTO cleanup = service.purgeExpiredLogs();

        assertEquals(2, cleanup.deletedFiles());
        assertTrue(cleanup.freedBytes() > 0);
        assertFalse(Files.exists(expiredByDate));
        assertFalse(Files.exists(expiredByMtime));
        assertTrue(Files.exists(activeApp));
        assertTrue(Files.exists(recentArchive));
    }

    private static byte[] gzip(String content) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(baos)) {
            gz.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return baos.toByteArray();
    }
}
