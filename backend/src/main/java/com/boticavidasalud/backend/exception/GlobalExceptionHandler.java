package com.boticavidasalud.backend.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ============================================================
    // ERRORES DE NEGOCIO
    // ============================================================

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(
            BusinessException ex) {

        String mensaje = ex.getMessage();

        String normalizado = mensaje == null
                ? ""
                : mensaje.toLowerCase(Locale.ROOT);

        boolean esConflicto =
                normalizado.contains(
                        "ya existe un cliente con ese número de documento"
                )
                || normalizado.contains("ya existe una categoría")
                || normalizado.contains("ya existe otra categoría")
                || normalizado.contains("ya existe un producto")
                || normalizado.contains("ya existe una presentación")
                || normalizado.contains("ya existe un lote");

        HttpStatus status = esConflicto
                ? HttpStatus.CONFLICT
                : HttpStatus.BAD_REQUEST;

        return buildResponse(status, mensaje);
    }

    // ============================================================
    // VALIDACIONES DE DTO
    // ============================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex) {

        String mensaje = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("Error de validación");

        return buildResponse(HttpStatus.BAD_REQUEST, mensaje);
    }

    // ============================================================
    // NO AUTENTICADO
    // ============================================================

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(
            AuthenticationException ex) {

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Debe autenticarse para acceder a este recurso"
        );
    }

    // ============================================================
    // SIN PERMISOS
    // ============================================================

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex) {

        return buildResponse(
                HttpStatus.FORBIDDEN,
                "No tiene permisos para realizar esta acción"
        );
    }

    // ============================================================
    // RECURSO NO ENCONTRADO
    // ============================================================

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(
            ResourceNotFoundException ex) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
    }

    // ============================================================
    // CONFLICTOS CON LA BASE DE DATOS
    // ============================================================

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(
            DataIntegrityViolationException ex) {

        String mensaje =
                "Conflicto con los datos. Verifique la información ingresada.";

        String detalle = mensajesDeLaExcepcion(ex)
                .toLowerCase(Locale.ROOT);

        if (detalle.contains("uq_presentacion_producto_tipo_factor")) {

            mensaje =
                    "Ya existe una presentación con ese tipo y factor para este producto";

        } else if (detalle.contains("uq_categoria_nombre")) {

            mensaje = "Ya existe una categoría con ese nombre";

        } else if (
                detalle.contains("uq_cliente_documento")
                || detalle.contains("numero_documento")) {

            mensaje =
                    "Ya existe un cliente con ese número de documento";

        } else if (detalle.contains("uq_producto_nombre")) {

            mensaje = "Ya existe un producto con ese nombre";
        }

        return buildResponse(HttpStatus.CONFLICT, mensaje);
    }

    // ============================================================
    // ERRORES NO CONTROLADOS
    // ============================================================

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(
            RuntimeException ex) {

        // No exponer detalles internos de la base de datos al cliente.
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno del servidor"
        );
    }

    // ============================================================
    // MÉTODO COMÚN PARA GENERAR RESPUESTAS
    // ============================================================

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status,
            String mensaje) {

        Map<String, Object> body = new HashMap<>();

        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("mensaje", mensaje);

        return ResponseEntity.status(status).body(body);
    }

    // ============================================================
    // RECUPERAR MENSAJES INTERNOS DE LA EXCEPCIÓN
    // ============================================================

    private String mensajesDeLaExcepcion(Throwable ex) {

        StringBuilder mensajes = new StringBuilder();
        Throwable actual = ex;

        while (actual != null) {

            if (actual.getMessage() != null) {
                mensajes.append(" ").append(actual.getMessage());
            }

            actual = actual.getCause();
        }

        return mensajes.toString();
    }
}


