package com.duolinfo.ia.proj.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.config.CorrelationIdFilter;
import com.duolinfo.ia.proj.dto.response.ApiErrorResponse;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.duolinfo.ia.proj.service.AuditLogService auditLogService;

    public GlobalExceptionHandler() {
    }

    public GlobalExceptionHandler(com.duolinfo.ia.proj.service.AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException exception,
            ServletWebRequest request) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage(), request, null);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(
            BusinessException exception,
            ServletWebRequest request) {
        return build(HttpStatus.CONFLICT, exception.getMessage(), request, null);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleOptimisticLocking(
            OptimisticLockingFailureException exception,
            ServletWebRequest request) {
        String msg = "O registro foi modificado simultaneamente por outro usuário ou transação. Recarregue os dados e tente novamente.";
        return build(HttpStatus.CONFLICT, msg, request, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(
            AuthenticationException exception,
            ServletWebRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Credenciais inválidas ou token de autenticação expirado.", request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException exception,
            ServletWebRequest request) {
        return build(HttpStatus.FORBIDDEN, "Acesso negado para o seu perfil.", request, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataConflict(
            DataIntegrityViolationException exception,
            ServletWebRequest request) {
        String message = resolveDataIntegrityMessage(exception);
        return build(HttpStatus.CONFLICT, message, request, null);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(
            ResponseStatusException exception,
            ServletWebRequest request) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String message = exception.getReason() == null
            ? status.getReasonPhrase()
            : exception.getReason();
        return build(status, message, request, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception,
            ServletWebRequest request) {
        return build(HttpStatus.BAD_REQUEST, exception.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            ServletWebRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        String summaryMessage = fieldErrors.entrySet().stream()
            .map(entry -> entry.getKey() + ": " + entry.getValue())
            .collect(Collectors.joining("; "));

        return build(HttpStatus.BAD_REQUEST, summaryMessage, request, fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            ServletWebRequest request) {
        String message = exception.getConstraintViolations().stream()
            .map(v -> v.getMessage())
            .distinct()
            .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message, request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            ServletWebRequest request) {
        return build(
            HttpStatus.BAD_REQUEST,
            "O parâmetro '" + exception.getName() + "' tem um formato inválido.",
            request,
            null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException exception,
            ServletWebRequest request) {
        return build(
            HttpStatus.BAD_REQUEST,
            "O parâmetro obrigatório '" + exception.getParameterName() + "' não foi informado.",
            request,
            null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            ServletWebRequest request) {
        return build(
            HttpStatus.METHOD_NOT_ALLOWED,
            "O método HTTP " + exception.getMethod() + " não é permitido para esta rota.",
            request,
            null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception,
            ServletWebRequest request) {
        return build(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "O tipo de conteúdo (Content-Type) da requisição não é suportado.",
            request,
            null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(
            HttpMessageNotReadableException exception,
            ServletWebRequest request) {
        return build(
            HttpStatus.BAD_REQUEST,
            "O corpo da requisição está vazio ou em formato inválido.",
            request,
            null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleUploadTooLarge(
            MaxUploadSizeExceededException exception,
            ServletWebRequest request) {
        return build(
            HttpStatus.PAYLOAD_TOO_LARGE,
            "O arquivo excede o tamanho máximo permitido.",
            request,
            null);
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(
            org.springframework.web.servlet.resource.NoResourceFoundException exception,
            ServletWebRequest request) {
        return build(
            HttpStatus.NOT_FOUND,
            "Recurso não encontrado.",
            request,
            null);
    }

    @ExceptionHandler(org.springframework.web.servlet.NoHandlerFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoHandlerFound(
            org.springframework.web.servlet.NoHandlerFoundException exception,
            ServletWebRequest request) {
        return build(
            HttpStatus.NOT_FOUND,
            "Rota não encontrada.",
            request,
            null);
    }

    @ExceptionHandler(org.springframework.http.InvalidMediaTypeException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidMediaType(
            org.springframework.http.InvalidMediaTypeException exception,
            ServletWebRequest request) {
        return build(
            HttpStatus.BAD_REQUEST,
            "Tipo de mídia (Content-Type) inválido.",
            request,
            null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(
            Exception exception,
            ServletWebRequest request) {

        String correlationId = resolveCorrelationId(request);
        LOGGER.error("[{}] Erro não tratado ao processar {} {}: {}",
            correlationId,
            request.getHttpMethod(),
            request.getRequest().getRequestURI(),
            exception.getMessage(),
            exception);

        if (auditLogService != null) {
            String actor = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anonymous";
            auditLogService.recordEvent(
                "SYSTEM",
                "ERROR",
                "UNHANDLED_EXCEPTION",
                actor,
                "SYSTEM",
                request.getRequest().getRequestURI(),
                null,
                "ERROR",
                request.getRequest().getRemoteAddr(),
                correlationId,
                exception.getClass().getSimpleName() + ": " + exception.getMessage()
            );
        }

        return build(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Ocorreu um erro interno no servidor. Código de rastreamento: " + correlationId,
            request,
            null
        );
    }

    private String resolveDataIntegrityMessage(DataIntegrityViolationException exception) {
        String rootMsg = exception.getMostSpecificCause() != null ? exception.getMostSpecificCause().getMessage() : "";
        String lower = rootMsg.toLowerCase();

        if (lower.contains("uk_student_language_name") || lower.contains("student_languages")) {
            return "Este idioma já está cadastrado para o estudante.";
        }
        if (lower.contains("uk_student_vocab_term") || lower.contains("student_vocabularies")) {
            return "Este termo já existe no seu vocabulário neste idioma.";
        }
        if (lower.contains("uk_active_enrollment")) {
            return "O aluno já possui uma matrícula ativa nesta turma.";
        }
        if (lower.contains("uk_active_class_teacher")) {
            return "O professor já está atribuído ativamente a esta turma.";
        }
        if (lower.contains("users_email") || lower.contains("email")) {
            return "O e-mail informado já está em uso por outro usuário.";
        }
        return "A operação viola uma restrição dos dados cadastrados.";
    }

    private String resolveCorrelationId(ServletWebRequest request) {
        Object attr = request.getAttribute(CorrelationIdFilter.REQUEST_ATTR, ServletWebRequest.SCOPE_REQUEST);
        if (attr instanceof String s && !s.isBlank()) {
            return s;
        }
        String mdc = MDC.get(CorrelationIdFilter.MDC_KEY);
        return (mdc != null && !mdc.isBlank()) ? mdc : "N/A";
    }

    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status,
            String message,
            ServletWebRequest request,
            Map<String, String> validationErrors) {

        String path = request.getRequest().getRequestURI();
        String correlationId = resolveCorrelationId(request);

        if (status.is5xxServerError()) {
            LOGGER.error("[{}] Falha 5xx em {} {}: {}", correlationId, request.getHttpMethod(), path, message);
        } else if (status.is4xxClientError()) {
            LOGGER.warn("[{}] Falha 4xx ({}) em {} {}: {}", correlationId, status.value(), request.getHttpMethod(), path, message);
        }

        ApiErrorResponse response = new ApiErrorResponse(
            LocalDateTime.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            path,
            correlationId,
            validationErrors
        );

        return ResponseEntity
            .status(status)
            .body(response);
    }
}
