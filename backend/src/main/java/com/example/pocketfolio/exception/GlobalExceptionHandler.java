package com.example.pocketfolio.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String error, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", error);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String error, String code, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", error);
        body.put("code", code);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "NotFound", ex.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> handleValidation(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, "ValidationError", ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException ex) {
        String code = ex.getCode() != null ? ex.getCode().name() : "VALIDATION_ERROR";
        return build(HttpStatus.BAD_REQUEST, "ValidationError", code, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraint(DataIntegrityViolationException ex) {
        return build(HttpStatus.BAD_REQUEST, "ConstraintViolation", ex.getMostSpecificCause().getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOthers(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "ServerError", ex.getMessage());
    }
}
