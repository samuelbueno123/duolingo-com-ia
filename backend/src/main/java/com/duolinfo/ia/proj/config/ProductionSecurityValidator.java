package com.duolinfo.ia.proj.config;

import java.util.Arrays;
import java.util.Set;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ProductionSecurityValidator implements InitializingBean {

    public static final String LOCAL_DEV_JWT_SECRET =
            "Qa_nMLnp6cCiMSew10ZOceKwt3UQZvfUbG25IL1jIybAuVUiFr-59qYXYBBBQjhf";
    public static final String LOCAL_DEV_DB_PASSWORD = "duolingo_dev_password";
    public static final String DEFAULT_WEAK_ADMIN_PASSWORD = "duadmin";

    private static final Set<String> FORBIDDEN_SECRETS = Set.of(
            LOCAL_DEV_JWT_SECRET,
            "changeme",
            "change-me",
            "secret",
            "default"
    );

    private final Environment environment;
    private final String jwtSecret;
    private final String dbPassword;
    private final String adminEmails;
    private final String adminInitialPassword;

    public ProductionSecurityValidator(
            Environment environment,
            @Value("${jwt.secret:}") String jwtSecret,
            @Value("${spring.datasource.password:}") String dbPassword,
            @Value("${app.admin-emails:}") String adminEmails,
            @Value("${app.admin.initial-password:}") String adminInitialPassword) {
        this.environment = environment;
        this.jwtSecret = jwtSecret;
        this.dbPassword = dbPassword;
        this.adminEmails = adminEmails;
        this.adminInitialPassword = adminInitialPassword;
    }

    @Override
    public void afterPropertiesSet() {
        if (isProductionProfile(environment)) {
            validateProductionSettings(jwtSecret, dbPassword, adminEmails, adminInitialPassword);
        }
    }

    public static boolean isProductionProfile(Environment env) {
        if (env == null) {
            return false;
        }
        return Arrays.stream(env.getActiveProfiles())
                .anyMatch(p -> "prod".equalsIgnoreCase(p) || "production".equalsIgnoreCase(p));
    }

    public static void validateProductionSettings(
            String jwtSecret,
            String dbPassword,
            String adminEmails,
            String adminInitialPassword) {
        if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.trim().length() < 32
                || FORBIDDEN_SECRETS.contains(jwtSecret.trim())) {
            throw new IllegalStateException(
                "Falha de segurança em produção: JWT_SECRET ausente, fraco ou igual ao segredo padrão de desenvolvimento."
            );
        }

        if (dbPassword == null || dbPassword.isBlank()
                || LOCAL_DEV_DB_PASSWORD.equals(dbPassword.trim())
                || "postgres".equalsIgnoreCase(dbPassword.trim())) {
            throw new IllegalStateException(
                "Falha de segurança em produção: POSTGRES_PASSWORD ausente ou utilizando credencial padrão de desenvolvimento."
            );
        }

        if (adminEmails == null || adminEmails.isBlank()) {
            throw new IllegalStateException(
                "Falha de segurança em produção: APP_ADMIN_EMAILS deve ser configurado explicitamente."
            );
        }

        if (adminInitialPassword == null || adminInitialPassword.isBlank()
                || DEFAULT_WEAK_ADMIN_PASSWORD.equals(adminInitialPassword.trim())
                || adminInitialPassword.trim().length() < 12) {
            throw new IllegalStateException(
                "Falha de segurança em produção: APP_ADMIN_INITIAL_PASSWORD deve ser definido explicitamente com pelo menos 12 caracteres e não pode ser 'duadmin'."
            );
        }
    }
}
