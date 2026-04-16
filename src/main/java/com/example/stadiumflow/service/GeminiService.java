package com.example.stadiumflow.service;

import com.example.stadiumflow.domain.Zone;
import com.example.stadiumflow.repository.ZoneRepository;

// Official Google Cloud Vertex AI Integration
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;

// Enterprise Google Cloud Infrastructure SDKs
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.stadiumflow.dto.AiResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for orchestrating AI-driven fan engagement.
 * Integrated with Google Vertex AI to provide real-time stadium analytics 
 * and conversational concierge services.
 */
@Service
public class GeminiService {
    
    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final ZoneRepository zoneRepository;

    // Google Gemini API Service (Primary - for Prompt Wars)
    @Autowired(required = false)
    private GeminiApiService geminiApiService;

    // Google Cloud Orchestration (Vertex AI Interface - Secondary)
    private VertexAI vertexAi;
    private GenerativeModel model;

    // Google Cloud Asset & Security Infrastructure
    private Storage storage;

    // Configuration fields
    private String stadiumApiKey;
    private String vertexProjectId;
    private String vertexLocation;
    private String vertexModelName;

    public GeminiService(ZoneRepository zoneRepository,
                         @Value("${vertex.ai.project-id}") String projectId,
                         @Value("${vertex.ai.location}") String location,
                         @Value("${vertex.ai.model-name}") String modelName,
                         @Value("${stadium.api.key}") String apiKey) {
        this.zoneRepository = zoneRepository;
        this.stadiumApiKey = apiKey;
        this.vertexProjectId = projectId;
        this.vertexLocation = location;
        this.vertexModelName = modelName;

        try {
            // Initialize Google Cloud Storage for potential file operations
            this.storage = StorageOptions.getDefaultInstance().getService();

            // Initialize Vertex AI with real configuration
            this.vertexAi = new VertexAI(vertexProjectId, vertexLocation);
            this.model = new GenerativeModel(vertexModelName, vertexAi);
            log.info("✅ Vertex AI initialized successfully: project={}, location={}, model={}",
                    vertexProjectId, vertexLocation, vertexModelName);
        } catch (Exception e) {
            log.warn("⚠️ Vertex AI initialization failed (will use fallback logic): {}", e.getMessage());
            // Graceful degradation - application continues without AI
        }
    }

    /**
     * Processes natural language queries against stadium telemetry data.
     * Priority: Google Gemini API > Vertex AI > Rule-based logic
     *
     * @param rawQuery The user input string.
     * @return An AiResponseDto containing the AI response and provider metadata.
     */
    public AiResponseDto processQuery(String rawQuery) {
        log.debug("Processing fan query: {}", rawQuery);

        // 🏆 PRIORITY 1: Try Google Gemini API (for Prompt Wars competition)
        if (geminiApiService != null && geminiApiService.isAvailable()) {
            try {
                String geminiResponse = geminiApiService.processWithGemini(rawQuery);
                if (geminiResponse != null && !geminiResponse.isEmpty()) {
                    log.info("✅ Using Google Gemini API (Prompt Wars)");
                    return new AiResponseDto(geminiResponse, "Google Gemini API");
                }
            } catch (Exception e) {
                log.warn("⚠️ Gemini API failed, trying Vertex AI: {}", e.getMessage());
            }
        }

        // PRIORITY 2: Try Vertex AI (if Gemini API unavailable)
        if (model != null && vertexAi != null) {
            try {
                return processWithVertexAI(rawQuery);
            } catch (Exception e) {
                log.error("❌ Vertex AI API call FAILED. Error type: {}, Message: {}",
                          e.getClass().getName(), e.getMessage(), e);
                // Fall through to rule-based logic
            }
        }

        // PRIORITY 3: Fallback - Rule-based logic
        return processWithRuleBasedLogic(rawQuery);
    }

    /**
     * Process query using Google Vertex AI (Gemini).
     */
    private AiResponseDto processWithVertexAI(String rawQuery) throws Exception {
        List<Zone> zones = zoneRepository.findAll();

        // Build context for Gemini
        StringBuilder context = new StringBuilder();
        context.append("You are a helpful stadium assistant for a Coldplay concert at Wankhede Stadium. ");
        context.append("Current stadium zones status:\n");
        for (Zone zone : zones) {
            context.append(String.format("- %s: %d min wait, %d%% capacity\n",
                zone.getName(), zone.getWaitTime(), zone.getDensity()));
        }
        context.append("\nUser question: ").append(rawQuery);
        context.append("\n\nProvide a helpful, concise response (max 2-3 sentences).");

        // Call Vertex AI
        log.info("🤖 Calling Vertex AI with prompt length: {} chars", context.length());
        var response = model.generateContent(context.toString());
        String aiResponse = ResponseHandler.getText(response);

        log.info("✅ Vertex AI response received: {} chars", aiResponse.length());
        return new AiResponseDto(aiResponse, "Google Vertex AI (Gemini " + vertexModelName + ")");
    }

    /**
     * Fallback: Rule-based query processing when Vertex AI is unavailable.
     */
    private AiResponseDto processWithRuleBasedLogic(String rawQuery) {
        String userQuery = rawQuery.toLowerCase();
        List<Zone> zones = zoneRepository.findAll();
        
        String responseText = "";

        // 1. DYNAMIC ZONE LOOKUP (Improved Partial Matching)
        Optional<Zone> mentionedZone = zones.stream()
            .filter(z -> {
                String name = z.getName().toLowerCase();
                String id = z.getId().toLowerCase().replace("_", " ");
                return userQuery.contains(id) || 
                       userQuery.contains(z.getId().toLowerCase()) ||
                       (userQuery.contains("north") && name.contains("north")) ||
                       (userQuery.contains("vip") && name.contains("vip")) ||
                       (userQuery.contains("food") && name.contains("food")) ||
                       (userQuery.contains("hydration") && name.contains("hydration")) ||
                       (userQuery.contains("gate a") && name.contains("gate a"));
            })
            .findFirst();

        if (mentionedZone.isPresent()) {
            Zone z = mentionedZone.get();
            responseText = "I see you're asking about " + z.getName() + ". It currently has a " + z.getWaitTime() + "m wait and is at " + z.getDensity() + "% capacity. ";
            if (z.getWaitTime() > 15) {
                responseText += "It's quite busy right now! I recommend waiting 10 minutes or using the 'Live Map' to find an alternate route.";
            } else {
                responseText += "The flow looks great there. It's a perfect time to head over!";
            }
        } 
        else if (userQuery.contains("status") || userQuery.contains("how is") || userQuery.contains("capacity") || userQuery.contains("overview")) {
            Zone busiest = zones.stream().max(Comparator.comparingInt(Zone::getWaitTime)).orElse(null);
            Zone emptiest = zones.stream().min(Comparator.comparingInt(Zone::getWaitTime)).orElse(null);
            
            responseText = "Here is your Stadium Health Report: ";
            if (busiest != null) responseText += "The main bottleneck is " + busiest.getName() + " (" + busiest.getWaitTime() + "m). ";
            if (emptiest != null) responseText += "For the fastest service, head to " + emptiest.getName() + " where the wait is only " + emptiest.getWaitTime() + "m.";
        }
        else if (userQuery.contains("deal") || userQuery.contains("cheap") || userQuery.contains("discount") || userQuery.contains("food")) {
            List<Zone> stands = zones.stream()
                .filter(z -> z.getId().contains("Section"))
                .sorted(Comparator.comparingInt(Zone::getWaitTime))
                .collect(java.util.stream.Collectors.toList());

            if (!stands.isEmpty() && stands.get(0).getWaitTime() < 5) {
                responseText = "Good news! " + stands.get(0).getName() + " has a very low wait (" + stands.get(0).getWaitTime() + "m), meaning our Dynamic Yield discount is currently active there. Head there for a deal!";
            } else {
                responseText = "Concessions are at normal capacity. Check the 'Order' tab for live price adjustments as queues change.";
            }
        }
        else {
            responseText = "I am the StadiumPulse Concert Concierge. You can ask me about entry gates, hydration points, or 'eco-deals' at our Food Villages for the Coldplay Music of the Spheres tour!";
        }

        return new AiResponseDto(responseText, "Rule-Based AI (Vertex AI unavailable)");
    }
}
