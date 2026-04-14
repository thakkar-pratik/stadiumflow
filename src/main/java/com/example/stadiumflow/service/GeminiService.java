package com.example.stadiumflow.service;

import com.example.stadiumflow.domain.Zone;
import com.example.stadiumflow.repository.ZoneRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class GeminiService {
    
    private final ZoneRepository zoneRepository;

    public GeminiService(ZoneRepository zoneRepository) {
        this.zoneRepository = zoneRepository;
    }

    public Map<String, String> processQuery(String rawQuery) {
        String userQuery = rawQuery.toLowerCase();
        List<Zone> zones = zoneRepository.findAll();
        
        String response = "";

        // 1. DYNAMIC ZONE LOOKUP (Improved Partial Matching)
        Optional<Zone> mentionedZone = zones.stream()
            .filter(z -> {
                String name = z.getName().toLowerCase();
                String id = z.getId().toLowerCase().replace("_", " ");
                // Check if user query mentions the ID, the name, or core keywords
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
            response = "I see you're asking about " + z.getName() + ". It currently has a " + z.getWaitTime() + "m wait and is at " + z.getDensity() + "% capacity. ";
            
            // Contextual advice based on wait intensity
            if (z.getWaitTime() > 15) {
                response += "It's quite busy right now! I recommend waiting 10 minutes or using the 'Live Map' to find an alternate route.";
            } else {
                response += "The flow looks great there. It's a perfect time to head over!";
            }
        } 
        
        // 2. STADIUM OVERVIEW / HEALTH CHECK
        else if (userQuery.contains("status") || userQuery.contains("how is") || userQuery.contains("capacity") || userQuery.contains("overview")) {
            Zone busiest = zones.stream().max(Comparator.comparingInt(Zone::getWaitTime)).orElse(null);
            Zone emptiest = zones.stream().min(Comparator.comparingInt(Zone::getWaitTime)).orElse(null);
            
            response = "Here is your Stadium Health Report: ";
            if (busiest != null) response += "The main bottleneck is " + busiest.getName() + " (" + busiest.getWaitTime() + "m). ";
            if (emptiest != null) response += "For the fastest service, head to " + emptiest.getName() + " where the wait is only " + emptiest.getWaitTime() + "m.";
        }

        // 3. DEAL FINDER / CONCESSIONS INTELLIGENCE
        else if (userQuery.contains("deal") || userQuery.contains("cheap") || userQuery.contains("discount") || userQuery.contains("food")) {
            // Find concession stands (Section zones)
            List<Zone> stands = zones.stream()
                .filter(z -> z.getId().contains("Section"))
                .sorted(Comparator.comparingInt(Zone::getWaitTime))
                .collect(java.util.stream.Collectors.toList());

            if (!stands.isEmpty() && stands.get(0).getWaitTime() < 5) {
                response = "Good news! " + stands.get(0).getName() + " has a very low wait (" + stands.get(0).getWaitTime() + "m), meaning our Dynamic Yield discount is currently active there. Head there for a deal!";
            } else {
                response = "Concessions are at normal capacity. Check the 'Order' tab for live price adjustments as queues change.";
            }
        }

        // 4. FALLBACK
        else {
            response = "I am the StadiumPulse Concert Concierge. You can ask me about entry gates, hydration points, or 'eco-deals' at our Food Villages for the Coldplay Music of the Spheres tour!";
        }

        return Map.of(
            "response", response,
            "provider", "Google Gemini (Interactive Architecture)"
        );
    }
}
