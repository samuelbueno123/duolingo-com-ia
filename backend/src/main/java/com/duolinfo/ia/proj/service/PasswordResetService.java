package com.duolinfo.ia.proj.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.request.ForgotPasswordRequestDTO;
import com.duolinfo.ia.proj.dto.request.ResetPasswordRequestDTO;
import com.duolinfo.ia.proj.entity.PasswordResetToken;
import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.repository.PasswordResetTokenRepository;
import com.duolinfo.ia.proj.repository.UserRepository;

@Service
@Transactional
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int EXPIRATION_MINUTES = 15;
    private static final int MAX_REQUESTS_PER_WINDOW = 5;
    private static final String GENERIC_RESPONSE_MESSAGE =
            "Se o e-mail informado estiver cadastrado, um link e token de recuperação (válidos por 15 minutos) foram enviados para o seu e-mail.";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    @Value("${spring.mail.host:}")
    private String smtpHost;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this(userRepository, tokenRepository, passwordEncoder, jwtService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EmailService emailService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    public Map<String, Object> requestPasswordReset(ForgotPasswordRequestDTO dto) {
        String normalizedEmail = normalizeEmail(dto != null ? dto.email() : null);
        if (normalizedEmail == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um e-mail válido.");
        }

        Instant now = Instant.now();
        Instant windowStart = now.minus(EXPIRATION_MINUTES, ChronoUnit.MINUTES);
        long recentRequests = tokenRepository.countByUser_EmailIgnoreCaseAndCreatedAtAfter(normalizedEmail, windowStart);
        if (recentRequests >= MAX_REQUESTS_PER_WINDOW) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas solicitações de recuperação recentes. Aguarde alguns minutos antes de tentar novamente."
            );
        }

        boolean emailSent = false;
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            List<PasswordResetToken> activeTokens =
                    tokenRepository.findByUser_EmailIgnoreCaseAndUsedAtIsNullOrderByCreatedAtDesc(normalizedEmail);
            for (PasswordResetToken existing : activeTokens) {
                existing.setUsedAt(now);
            }
            tokenRepository.saveAll(activeTokens);

            String rawJwtToken = jwtService.generatePasswordResetToken(user);
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUser(user);
            resetToken.setTokenHash(hashToken(rawJwtToken));
            resetToken.setCreatedAt(now);
            resetToken.setExpiresAt(now.plus(EXPIRATION_MINUTES, ChronoUnit.MINUTES));
            resetToken.setUsedAt(null);
            resetToken.setAttemptCount(0);
            tokenRepository.save(resetToken);

            if (emailService != null) {
                emailSent = emailService.sendPasswordResetEmail(normalizedEmail, rawJwtToken, EXPIRATION_MINUTES);
            } else {
                log.info("[PASSWORD-RESET] EmailService indisponível. Token JWT gerado para {} (expira em {} min): {}",
                        normalizedEmail, EXPIRATION_MINUTES, rawJwtToken);
            }
        }

        boolean mailConfigured = smtpHost != null && !smtpHost.isBlank();
        return Map.of(
                "message", GENERIC_RESPONSE_MESSAGE,
                "expiresInMinutes", EXPIRATION_MINUTES,
                "mailConfigured", mailConfigured,
                "emailSent", emailSent
        );
    }

    public Map<String, Object> resetPassword(ResetPasswordRequestDTO dto) {
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados de redefinição são obrigatórios.");
        }
        String normalizedEmail = normalizeEmail(dto.email());
        String rawToken = dto.token() != null ? dto.token().trim() : "";
        String newPassword = dto.newPassword() != null ? dto.newPassword().trim() : "";

        if (rawToken.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de recuperação é obrigatório.");
        }
        if (newPassword.length() < 6 || newPassword.length() > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A nova senha deve ter entre 6 e 72 caracteres.");
        }
        if (!jwtService.isPasswordResetTokenValid(rawToken)) {
            throw invalidResetToken();
        }

        String tokenEmail;
        Optional<Long> userId;
        try {
            tokenEmail = normalizeEmail(jwtService.getEmailFromPasswordResetToken(rawToken));
            userId = jwtService.getUserIdFromPasswordResetToken(rawToken);
        } catch (RuntimeException exception) {
            throw invalidResetToken();
        }
        if (tokenEmail == null || userId.isEmpty()) {
            throw invalidResetToken();
        }
        if (normalizedEmail != null && !tokenEmail.equals(normalizedEmail)) {
            throw invalidResetToken();
        }

        Instant now = Instant.now();
        String candidateHash = hashToken(rawToken);
        Optional<PasswordResetToken> tokenOpt = tokenRepository
                .findFirstByUser_IdAndTokenHashAndUsedAtIsNullOrderByCreatedAtDesc(userId.get(), candidateHash);
        if (tokenOpt.isEmpty()) {
            throw invalidResetToken();
        }

        PasswordResetToken resetToken = tokenOpt.get();
        if (resetToken.getExpiresAt() == null || resetToken.getExpiresAt().isBefore(now)
                || tokenRepository.markUsedIfUnusedAndNotExpired(resetToken.getId(), now) != 1) {
            throw invalidResetToken();
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return Map.of(
                "message", "Senha redefinida com sucesso. Você já pode entrar com sua nova senha."
        );
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private ResponseStatusException invalidResetToken() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Código de recuperação inválido ou expirado."
        );
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim().toLowerCase();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
