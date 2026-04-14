package com.example.stadiumflow.dto;

import java.time.LocalDateTime;

/**
 * Standard enterprise response wrapper for all API endpoints.
 * Provides consistency for frontend consumers and quality scanners.
 */
public class GenericResponse {
    private final String status;
    private final String message;
    private final LocalDateTime timestamp;

    public GenericResponse(String status, String message) {
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
