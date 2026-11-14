package com.duolinfo.ia.proj;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:flyway_schema_validation;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.sql.init.mode=never"
})
class FlywaySchemaValidationTest {

    private static Path tempMigrationDir;

    @DynamicPropertySource
    static void configureFlywayLocations(DynamicPropertyRegistry registry) throws Exception {
        tempMigrationDir = Files.createTempDirectory("duolinfo-flyway-h2-");
        Path sourceDir = Path.of("src/main/resources/db/migration");
        try (Stream<Path> files = Files.list(sourceDir)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                String sql = Files.readString(file, StandardCharsets.UTF_8);
                // H2 2.x em modo PostgreSQL não suporta predicados WHERE em índices parciais.
                // Mantemos V1, V3 e V4 idênticos e adaptamos apenas as 2 cláusulas WHERE parciais em V2 para validação de schema.
                if (file.getFileName().toString().startsWith("V2__")) {
                    sql = sql
                        .replace("    WHERE status = 'ACTIVE';", ";")
                        .replace("    WHERE end_date IS NULL;", ";");
                }
                Files.writeString(tempMigrationDir.resolve(file.getFileName()), sql, StandardCharsets.UTF_8);
            }
        }
        registry.add("spring.flyway.locations", () -> "filesystem:" + tempMigrationDir.toAbsolutePath());
    }

    @AfterAll
    static void cleanupTempMigrations() throws Exception {
        if (tempMigrationDir != null && Files.exists(tempMigrationDir)) {
            try (Stream<Path> walk = Files.walk(tempMigrationDir)) {
                for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(p);
                }
            }
        }
    }

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Valida que todas as migrations Flyway V1-V4 aplicam e batem 100% com o schema JPA (ddl-auto=validate)")
    void flywayMigrationsMatchJpaEntitiesSchema() throws Exception {
        Set<String> userColumns = new HashSet<>();
        Set<String> flywayVersions = new HashSet<>();
        try (Connection conn = dataSource.getConnection()) {
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, "users", null)) {
                while (rs.next()) {
                    userColumns.add(rs.getString("COLUMN_NAME").toLowerCase());
                }
            }
            try (var st = conn.createStatement();
                 var rs = st.executeQuery("SELECT version FROM flyway_schema_history WHERE success = TRUE")) {
                while (rs.next()) {
                    flywayVersions.add(rs.getString(1));
                }
            }
        }
        assertTrue(flywayVersions.containsAll(Set.of("1", "2", "3", "4")));
        assertTrue(userColumns.contains("ui_language"));
        assertTrue(userColumns.contains("theme_mode"));
        assertTrue(userColumns.contains("daily_goal_minutes"));
        assertTrue(userColumns.contains("notif_daily_reminders"));
        assertTrue(userColumns.contains("notif_achievements"));
        assertTrue(userColumns.contains("notif_ranking"));
        assertTrue(userColumns.contains("notif_class_updates"));
    }
}
