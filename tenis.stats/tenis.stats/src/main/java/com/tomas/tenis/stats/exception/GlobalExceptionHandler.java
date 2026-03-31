package com.tomas.tenis.stats.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 1. Captura errores de validación (Ej: @NotBlank, @Size)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new HashMap<>();
        Map<String, String> erroresTecnicos = new HashMap<>();

        // Extraemos qué campo falló y qué mensaje tiene
        ex.getBindingResult().getFieldErrors().forEach(error ->
                erroresTecnicos.put(error.getField(), error.getDefaultMessage())
        );

        body.put("timestamp", LocalDateTime.now());
        body.put("estado", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Error de Validación");
        body.put("detalles", erroresTecnicos);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // 2. Tu método de RuntimeException (Mejorado con timestamp)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Object> handleRuntimeException(RuntimeException exception) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("mensaje", exception.getMessage());
        body.put("estado", "Error de Negocio");

        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    // 3. Catch-all: Para cualquier otro error inesperado (Evita el Whitelabel 500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGlobalException(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("mensaje", "Ocurrió un error inesperado en el servidor");
        body.put("error", ex.getMessage());

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}