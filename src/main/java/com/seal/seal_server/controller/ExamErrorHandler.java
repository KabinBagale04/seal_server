package com.seal.seal_server.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

/** Scoped to new controllers so existing auth/admin error contracts are unchanged. */
@RestControllerAdvice(assignableTypes = {ExamController.class, StudentExamController.class, GradingController.class})
public class ExamErrorHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> status(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason() == null ? "Request failed." : e.getReason()));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> conflict(DataIntegrityViolationException e) {
        return ResponseEntity.status(409).body(Map.of("message", "A conflicting record already exists. Please retry."));
    }
}
