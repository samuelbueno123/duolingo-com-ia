package com.duolinfo.ia.proj.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.response.ApiErrorResponse;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException exception,
            ServletWebRequest request) {

        return build(
            HttpStatus.NOT_FOUND,
            exception.getMessage(),
            request
        );
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(
            BusinessException exception,
            ServletWebRequest request) {

        return build(
            HttpStatus.CONFLICT,
            exception.getMessage(),
            request
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException exception,
            ServletWebRequest request) {
        return build(
            HttpStatus.FORBIDDEN,
            "Acesso negado para o seu perfil.",
            request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataConflict(
            DataIntegrityViolationException exception,
            ServletWebRequest request) {
        return build(
            HttpStatus.CONFLICT,
            "A operação viola uma restrição dos dados cadastrados.",
            request
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(
            ResponseStatusException exception,
            ServletWebRequest request) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String message = exception.getReason() == null
            ? status.getReasonPhrase()
            : exception.getReason();
        return build(status, message, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception,
            ServletWebRequest request) {
        return build(
            HttpStatus.BAD_REQUEST,
            exception.getMessage(),
            request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            ServletWebRequest request) {

        String message =
            exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                    error.getField()
                    + ": "
                    + error.getDefaultMessage()
                )
                .collect(Collectors.joining("; "));

        return build(
            HttpStatus.BAD_REQUEST,
            message,
            request
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            ServletWebRequest request) {
        String message = exception.getConstraintViolations().stream()
            .map(violation -> violation.getMessage())
            .distinct()
            .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            ServletWebRequest request) {
        return build(
            HttpStatus.BAD_REQUEST,
            "O parâmetro '" + exception.getName() + "' tem um formato inválido.",
            request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(
            HttpMessageNotReadableException exception,
            ServletWebRequest request) {
        return build(
            HttpStatus.BAD_REQUEST,
            "O corpo da requisição está vazio ou em formato inválido.",
            request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleUploadTooLarge(
            MaxUploadSizeExceededException exception,
            ServletWebRequest request) {
        return build(
            HttpStatus.PAYLOAD_TOO_LARGE,
            "O arquivo excede o tamanho máximo permitido.",
            request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(
            Exception exception,
            ServletWebRequest request) {

        LOGGER.error("Erro inesperado ao processar {} {}",
            request.getHttpMethod(),
            request.getRequest().getRequestURI(),
            exception);
        return build(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Ocorreu um erro interno no servidor.",
            request
        );
    }

    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status,
            String message,
            ServletWebRequest request) {

        String path =
            request.getRequest().getRequestURI();

        ApiErrorResponse response =
            new ApiErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
            );

        return ResponseEntity
            .status(status)
            .body(response);
    }
}
