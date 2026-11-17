package com.duolinfo.ia.proj.config;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Detecta e repara de forma idempotente strings UTF-8 duplamente codificadas
 * (ex: "InglÃªs" -> "Inglês") persistidas no PostgreSQL por scripts ou clientes legados.
 */
@Component
@Order(110)
public class Utf8MojibakeSanitizer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(Utf8MojibakeSanitizer.class);
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    private final JdbcTemplate jdbcTemplate;

    public Utf8MojibakeSanitizer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isPostgreSql()) {
            return;
        }
        try {
            int updatedValues = repairAllPostgresTextColumns();
            if (updatedValues > 0) {
                log.info("Utf8MojibakeSanitizer reparou {} valor(es) com acentuação corrompida no PostgreSQL.", updatedValues);
            }
        } catch (Exception ex) {
            log.warn("Utf8MojibakeSanitizer ignorado com segurança: {}", ex.getMessage());
        }
    }

    private boolean isPostgreSql() {
        DataSource dataSource = jdbcTemplate.getDataSource();
        if (dataSource == null) {
            return false;
        }
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            String productName = meta.getDatabaseProductName();
            return productName != null && productName.toLowerCase().contains("postgresql");
        } catch (Exception ex) {
            return false;
        }
    }

    private int repairAllPostgresTextColumns() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList("""
            SELECT table_name, column_name
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name <> 'flyway_schema_history'
              AND data_type IN ('character varying', 'text', 'character')
            ORDER BY table_name, ordinal_position
            """);

        int totalUpdates = 0;
        for (Map<String, Object> col : columns) {
            String table = String.valueOf(col.get("table_name"));
            String column = String.valueOf(col.get("column_name"));
            String quotedTable = "\"" + table.replace("\"", "\"\"") + "\"";
            String quotedColumn = "\"" + column.replace("\"", "\"\"") + "\"";

            String selectSql = "SELECT DISTINCT " + quotedColumn + " FROM " + quotedTable
                + " WHERE " + quotedColumn + " IS NOT NULL"
                + " AND (" + quotedColumn + " LIKE '%Ã%' OR " + quotedColumn + " LIKE '%Â%' OR " + quotedColumn + " LIKE '%â€%')";
            List<String> candidates = jdbcTemplate.queryForList(selectSql, String.class);
            for (String candidate : candidates) {
                String repaired = repairIfMojibake(candidate);
                if (!repaired.equals(candidate)) {
                    String updateSql = "UPDATE " + quotedTable + " SET " + quotedColumn + " = ? WHERE " + quotedColumn + " = ?";
                    try {
                        totalUpdates += jdbcTemplate.update(updateSql, repaired, candidate);
                    } catch (Exception ex) {
                        log.debug("Ignorando atualização de {}.{}: {}", table, column, ex.getMessage());
                    }
                }
            }
        }
        return totalUpdates;
    }

    public static String repairIfMojibake(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String current = value;
        for (int pass = 0; pass < 2; pass++) {
            String next = tryRepairOnce(current);
            if (next.equals(current)) {
                break;
            }
            current = next;
        }
        return current;
    }

    private static String tryRepairOnce(String value) {
        String repaired = tryStrictTranscode(value, StandardCharsets.ISO_8859_1);
        if (repaired != null && !repaired.equals(value) && repaired.length() < value.length()) {
            return repaired;
        }
        repaired = tryStrictTranscode(value, WINDOWS_1252);
        if (repaired != null && !repaired.equals(value) && repaired.length() < value.length()) {
            return repaired;
        }
        return value;
    }

    private static String tryStrictTranscode(String input, Charset sourceCharset) {
        try {
            CharsetEncoder encoder = sourceCharset.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
            ByteBuffer bytes = encoder.encode(CharBuffer.wrap(input));
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
            String decoded = decoder.decode(bytes).toString();
            if (decoded.indexOf('\uFFFD') >= 0) {
                return null;
            }
            return decoded;
        } catch (CharacterCodingException ex) {
            return null;
        }
    }
}
