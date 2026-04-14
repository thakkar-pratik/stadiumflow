package com.example.stadiumflow.controller;

import com.example.stadiumflow.dto.AiResponseDto;
import com.example.stadiumflow.service.GeminiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * Controller for fan-facing AI services.
 * Orchestrates natural language processing for stadium concierge features.
 */
@RestController
@RequestMapping("/api/ai")
public class GeminiController {

    private static final Logger log = LoggerFactory.getLogger(GeminiController.class);
    private final GeminiService geminiService;

    public GeminiController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    /**
     * Handles incoming AI assistant queries.
     * 
     * @param request The query payload.
     * @return A ResponseEntity containing the AiResponseDto.
     */
    @PostMapping("/ask")
    public ResponseEntity<AiResponseDto> askAssistant(@RequestBody Map<String, String> request) {
        String query = request.getOrDefault("query", "");
        log.info("AI Service Request: Processing fan query for StadiumPulse intelligence.");
        return ResponseEntity.ok(geminiService.processQuery(query));
    }
}
