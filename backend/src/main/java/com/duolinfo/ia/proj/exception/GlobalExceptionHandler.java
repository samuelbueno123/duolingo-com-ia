package com.duolinfo.ia.proj.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;

import com.duolinfo.ia.proj.dto.response.ApiErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(
            Exception exception,
            ServletWebRequest request) {

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