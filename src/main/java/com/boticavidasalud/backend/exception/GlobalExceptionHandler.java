package com.boticavidasalud.backend.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404 - Recurso no encontrado
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // 400 - Error de negocio (duplicados, estado inválido, etc.)
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // 400 - Validaciones de DTO (@NotBlank, @NotNull, etc.)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("Error de validación");
        return buildResponse(HttpStatus.BAD_REQUEST, mensaje);
    }

    // 500 - Errores no controlados
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        // No exponer detalles internos (SQL) al cliente
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }

    // 409 - Conflicto con restricciones únicas (mensaje amigable, sin SQL)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String mensaje = "Conflicto con los datos. Verifique la información ingresada.";
        String fullMsg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";

        if (fullMsg.contains("uq_presentacion_producto_tipo_factor")) {
            mensaje = "Ya existe una presentación con ese tipo y factor para este producto";
        } else if (fullMsg.contains("uq_categoria_nombre")) {
            mensaje = "Ya existe una categoría con ese nombre";
        } else if (fullMsg.contains("uq_cliente_documento")) {
            mensaje = "Ya existe un cliente con ese número de documento";
        } else if (fullMsg.contains("uq_producto_nombre")) {
            mensaje = "Ya existe un producto con ese nombre";
        }

        return buildResponse(HttpStatus.CONFLICT, mensaje);
    }

    // Método auxiliar
    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String mensaje) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("mensaje", mensaje);
        return ResponseEntity.status(status).body(body);
    }
}


