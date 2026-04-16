package com.example.stadiumflow.service;

import com.example.stadiumflow.domain.Zone;
import com.example.stadiumflow.repository.ZoneRepository;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Service for Google Gemini API integration
 * Used for Prompt Wars competition - demonstrates Google AI services
 */
@Service
public class GeminiApiService {
    
    private static final Logger logger = LoggerFactory.getLogger(GeminiApiService.class);
    
    @Value("${gemini.api.key}")
    private String apiKey;
    
    private final ZoneRepository zoneRepository;
    private GenerativeModel model;
    
    public GeminiApiService(ZoneRepository zoneRepository) {
        this.zoneRepository = zoneRepository;
    }
    
    @PostConstruct
    public void initialize() {
        try {
            if (apiKey != null && !apiKey.isEmpty() && !apiKey.equals("placeholder")) {
                model = new GenerativeModel("gemini-pro", apiKey);
                logger.info("✅ Google Gemini API initialized successfully!");
                logger.info("🏆 Using Google Gemini API for Prompt Wars competition");
            } else {
                logger.warn("⚠️ Gemini API key not configured. Set GEMINI_API_KEY environment variable.");
            }
        } catch (Exception e) {
            logger.error("❌ Failed to initialize Gemini API: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Process query using Google Gemini API
     * @param query User query
     * @return AI-generated response or null if unavailable
     */
    public String processWithGemini(String query) {
        if (model == null) {
            logger.debug("Gemini API not available");
            return null;
        }
        
        try {
            // Get current stadium data for context
            List<Zone> zones = zoneRepository.findAll();
            StringBuilder context = new StringBuilder();
            
            context.append("You are StadiumPulse AI, an intelligent assistant for the Coldplay 'Music of the Spheres' concert at a major stadium.\n\n");
            context.append("CURRENT REAL-TIME STADIUM DATA:\n");
            
            for (Zone zone : zones) {
                int capacity = zone.getCapacity();
                int current = zone.getCurrentCount();
                int waitTime = zone.getWaitTime();
                double utilization = capacity > 0 ? (current * 100.0 / capacity) : 0;
                
                context.append(String.format("- %s: %d/%d people (%.1f%% full), %d min wait\n", 
                    zone.getName(), current, capacity, utilization, waitTime));
            }
            
            context.append("\nUSER QUERY: ").append(query);
            context.append("\n\nINSTRUCTIONS: Provide a helpful, friendly, and concise response about the stadium status. ");
            context.append("Include specific zone names, wait times, and actionable recommendations. ");
            context.append("Be enthusiastic about the Coldplay concert!");
            
            logger.info("🤖 Calling Google Gemini API...");
            
            // Call Gemini API
            GenerateContentResponse response = model.generateContent(context.toString());
            String text = response.getText();
            
            if (text != null && !text.isEmpty()) {
                logger.info("✅ Gemini API response received ({} chars)", text.length());
                return text;
            } else {
                logger.warn("⚠️ Gemini API returned empty response");
                return null;
            }
            
        } catch (Exception e) {
            logger.error("❌ Gemini API call failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if Gemini API is available
     * @return true if initialized and ready
     */
    public boolean isAvailable() {
        return model != null;
    }
    
    /**
     * Get the API key status (for debugging)
     * @return masked API key or status message
     */
    public String getApiKeyStatus() {
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("placeholder")) {
            return "Not configured";
        }
        return "Configured (***" + apiKey.substring(Math.max(0, apiKey.length() - 4)) + ")";
    }
}
