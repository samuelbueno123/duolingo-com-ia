package com.duolinfo.ia.proj.service;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.host:}")
    private String smtpHost;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    @Value("${app.mail.from:}")
    private String fromEmail;

    @Value("${app.frontend.url:}")
    private String frontendUrl;

    @Value("${FRONTEND_HOST:${app.frontend.host:localhost}}")
    private String frontendHost;

    @Value("${FRONTEND_PORT:${app.frontend.port:3000}}")
    private String frontendPort;

    @Value("${FRONTEND_PROTOCOL:${app.frontend.protocol:http}}")
    private String frontendProtocol;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean sendPasswordResetEmail(String toEmail, String resetToken, int expirationMinutes) {
        String resetLink = buildResetLink(toEmail, resetToken);

        boolean mailConfigured = smtpHost != null && !smtpHost.isBlank() && mailSender != null;
        if (!mailConfigured) {
            log.info("[PASSWORD-RESET-EMAIL] SMTP não configurado. Link de redefinição gerado para {}: {}",
                    toEmail, resetLink);
            log.info("[PASSWORD-RESET-EMAIL] Token JWT (válido por {} min): {}", expirationMinutes, resetToken);
            return false;
        }

        try {
            log.info("[PASSWORD-RESET-EMAIL] Disparando envio de e-mail para {} via SMTP host {}...", toEmail, smtpHost);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            String sender = resolveSenderEmail();
            helper.setFrom(sender, "Duolinfo");
            helper.setReplyTo(sender);
            helper.setTo(toEmail);
            helper.setSubject("Redefinição de Senha - Duolinfo");

            String plainText = buildPlainTextEmail(toEmail, resetLink, expirationMinutes);
            String htmlText = buildHtmlEmail(toEmail, resetLink, expirationMinutes);
            helper.setText(plainText, htmlText);

            message.setHeader("Auto-Submitted", "auto-generated");
            message.setHeader("X-Auto-Response-Suppress", "All");

            mailSender.send(message);
            log.info("[PASSWORD-RESET-EMAIL] E-mail de redefinição enviado com sucesso para {}", toEmail);
            return true;
        } catch (Exception e) {
            log.error("[PASSWORD-RESET-EMAIL] Falha ao enviar e-mail para {}: {}", toEmail, e.getMessage(), e);
            return false;
        }
    }

    public String buildResetLink(String email, String token) {
        String base = getBaseFrontendUrl();
        return UriComponentsBuilder.fromUriString(base)
                .queryParam("token", token)
                .queryParam("email", email)
                .build()
                .toUriString();
    }

    private String getBaseFrontendUrl() {
        if (frontendUrl != null && !frontendUrl.isBlank()) {
            return frontendUrl.trim();
        }
        String protocol = (frontendProtocol != null && !frontendProtocol.isBlank()) ? frontendProtocol.trim() : "http";
        String host = (frontendHost != null && !frontendHost.isBlank()) ? frontendHost.trim() : "localhost";
        String port = (frontendPort != null && !frontendPort.isBlank()) ? frontendPort.trim() : "3000";

        if (("80".equals(port) && "http".equalsIgnoreCase(protocol)) ||
            ("443".equals(port) && "https".equalsIgnoreCase(protocol)) ||
            port.isEmpty()) {
            return protocol + "://" + host;
        }
        return protocol + "://" + host + ":" + port;
    }

    private String buildHtmlEmail(String email, String resetLink, int expirationMinutes) {
        return """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
              <meta charset="UTF-8">
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f7f8fc; margin: 0; padding: 24px; color: #1e293b; }
                .container { max-width: 560px; margin: 0 auto; background: #ffffff; border-radius: 16px; padding: 32px; border: 1px solid #e2e8f0; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); }
                .logo { font-size: 24px; font-weight: 800; color: #3579e8; margin-bottom: 24px; }
                h1 { font-size: 20px; font-weight: 700; color: #0f172a; margin-top: 0; margin-bottom: 12px; }
                p { font-size: 14px; line-height: 1.6; color: #475569; margin: 0 0 16px 0; }
                .btn { display: inline-block; background-color: #3579e8; color: #ffffff !important; text-decoration: none; padding: 13px 30px; border-radius: 10px; font-weight: 600; font-size: 15px; margin: 20px 0; }
                .footer { font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; padding-top: 16px; margin-top: 24px; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="logo">Duolinfo</div>
                <h1>Redefinição de Senha</h1>
                <p>Olá,</p>
                <p>Recebemos uma solicitação para redefinir a senha da sua conta vinculada ao e-mail <strong>%s</strong>.</p>
                <p>Para cadastrar sua nova senha, clique no botão abaixo:</p>
                <div style="text-align: center;">
                  <a href="%s" class="btn" target="_blank">Redefinir Minha Senha</a>
                </div>
                <p style="font-size: 13px; color: #64748b;">Ou acesse diretamente pelo link no seu navegador:</p>
                <p style="font-size: 12px; word-break: break-all;"><a href="%s" style="color: #3579e8;">%s</a></p>
                <p style="font-size: 12px; color: #dc2626; margin-top: 20px;"><strong>Importante:</strong> Este link é válido por %d minutos e só pode ser utilizado uma única vez.</p>
                <div class="footer">
                  Se você não solicitou a recuperação de senha, ignore este e-mail com segurança.
                </div>
              </div>
            </body>
            </html>
            """.formatted(email, resetLink, resetLink, resetLink, expirationMinutes);
    }

    private String resolveSenderEmail() {
        if (fromEmail != null && !fromEmail.isBlank() && !fromEmail.equalsIgnoreCase("nao-responda@duolingo.com")) {
            return fromEmail.trim();
        }
        if (smtpUsername != null && !smtpUsername.isBlank() && smtpUsername.contains("@")) {
            return smtpUsername.trim();
        }
        return (fromEmail != null && !fromEmail.isBlank()) ? fromEmail.trim() : "nao-responda@duolingo.com";
    }

    private String buildPlainTextEmail(String email, String resetLink, int expirationMinutes) {
        return """
            Olá,

            Recebemos uma solicitação para redefinir a senha da sua conta no Duolinfo vinculada ao e-mail %s.

            Para cadastrar sua nova senha, acesse o link abaixo no seu navegador:
            %s

            Importante:
            - Este link é válido por %d minutos e só pode ser utilizado uma única vez.
            - Se você não solicitou a recuperação de senha, ignore este e-mail com segurança.

            Equipe Duolinfo
            """.formatted(email, resetLink, expirationMinutes);
    }
}
