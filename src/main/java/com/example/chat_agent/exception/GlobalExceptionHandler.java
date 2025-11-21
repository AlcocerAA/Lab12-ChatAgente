package com.example.chat_agent.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAllExceptions(Exception e) {
        e.printStackTrace();

        String message = "Error global en el servidor: " + e.getClass().getSimpleName()
                + " - " + (e.getMessage() != null ? e.getMessage() : "sin mensaje");

        return ResponseEntity.ok(message);
    }
}
