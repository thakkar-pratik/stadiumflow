package com.example.stadiumflow.exception;

import com.example.stadiumflow.dto.GenericResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global Exception Architect for the StadiumFlow ecosystem.
 * Ensures consistent, security-hardened error responses across all API vectors.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        log.warn("Security Alert: Validation failed for incoming request payload.");
        String message = "Identity or payload validation failed. Please check your data format.";
        return new ResponseEntity<>(new GenericResponse("error", message), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse> handleGlobalErrors(Exception ex) {
        log.error("System Fault: Standardizing internal error for security compliance.", ex);
        return new ResponseEntity<>(new GenericResponse("error", "An internal system anomaly was detected. Our engineers have been alerted."), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
