package com.uniremington.marketplace.controller.res;

import com.uniremington.marketplace.service.GeminiChatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ChatRestController.class)
public class ChatControllerAdvice {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatControllerAdvice.class);

    public record ErrorResponse(String error) {
    }

    @ExceptionHandler(GeminiChatException.class)
    public ResponseEntity<ErrorResponse> handleChatException(GeminiChatException exception) {
        LOGGER.warn("Solicitud de chat rechazada: HTTP {}", exception.status().value());
        return ResponseEntity.status(exception.status())
                .body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("El mensaje no es válido.");
        return ResponseEntity.badRequest().body(new ErrorResponse(message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("El formato del mensaje no es válido."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        LOGGER.error("Error inesperado en el endpoint del chat: {}", exception.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("El chat no está disponible en este momento."));
    }
}
