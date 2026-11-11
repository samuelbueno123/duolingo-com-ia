package com.duolinfo.ia.proj.config;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro de identificação de requisições e rastreabilidade distribuída.
 * Injeta um Correlation ID único no MDC (Mapped Diagnostic Context) do SLF4J,
 * adiciona headers na resposta HTTP e realiza o log estruturado do ciclo de vida da requisição.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CorrelationIdFilter.class);

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String MDC_KEY = "correlationId";
    public static final String REQUEST_ATTR = "correlationId";

    private static final Pattern VALID_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{8,64}$");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        String correlationId = extractOrGenerateCorrelationId(request);

        MDC.put(MDC_KEY, correlationId);
        request.setAttribute(REQUEST_ATTR, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        response.setHeader(REQUEST_ID_HEADER, correlationId);

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String remoteIp = resolveClientIp(request);

        // Não loga ruído de preflight OPTIONS ou assets estáticos
        boolean isDiagnosticPath = uri.startsWith("/api/") || uri.startsWith("/actuator/");
        if (isDiagnosticPath && !"OPTIONS".equalsIgnoreCase(method)) {
            LOGGER.info("HTTP Request: method={} uri={} remoteIp={} correlationId={}",
                method, uri, remoteIp, correlationId);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;
            int status = response.getStatus();

            if (isDiagnosticPath && !"OPTIONS".equalsIgnoreCase(method)) {
                if (status >= 500) {
                    LOGGER.error("HTTP Response [SERVER ERROR]: method={} uri={} status={} durationMs={} correlationId={}",
                        method, uri, status, durationMs, correlationId);
                } else if (status >= 400) {
                    LOGGER.warn("HTTP Response [CLIENT ERROR]: method={} uri={} status={} durationMs={} correlationId={}",
                        method, uri, status, durationMs, correlationId);
                } else {
                    LOGGER.info("HTTP Response: method={} uri={} status={} durationMs={} correlationId={}",
                        method, uri, status, durationMs, correlationId);
                }
            }

            MDC.remove(MDC_KEY);
        }
    }

    private String extractOrGenerateCorrelationId(HttpServletRequest request) {
        String header = request.getHeader(CORRELATION_ID_HEADER);
        if (header == null || header.isBlank()) {
            header = request.getHeader(REQUEST_ID_HEADER);
        }

        if (header != null) {
            String trimmed = header.trim();
            if (VALID_ID_PATTERN.matcher(trimmed).matches()) {
                return trimmed;
            }
        }

        return UUID.randomUUID().toString();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
