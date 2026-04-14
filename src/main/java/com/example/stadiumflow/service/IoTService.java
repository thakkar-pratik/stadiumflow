package com.example.stadiumflow.service;

import com.example.stadiumflow.domain.Zone;
import com.example.stadiumflow.repository.ZoneRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class IoTService {

    private final ZoneRepository zoneRepository;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final Random random = new Random();

    public IoTService(ZoneRepository zoneRepository) {
        this.zoneRepository = zoneRepository;
    }

    @PostConstruct
    public void seedDatabase() {
        zoneRepository.save(new Zone("Gate_A", "Gate A", 5, 20));
        zoneRepository.save(new Zone("Gate_C", "Gate C", 5, 20));
        zoneRepository.save(new Zone("Section_112", "Section 112", 5, 20));
        zoneRepository.save(new Zone("Section_120", "Section 120", 5, 20));
        System.out.println("✅ JPA: Initialized 4 Stadium Zones in H2 Database.");
    }

    public SseEmitter registerClient() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        this.emitters.add(emitter);
        
        // IMMEDIATE FLUSH: Send current data to bypass Cloud Run/Nginx buffering
        try {
            emitter.send(zoneRepository.findAll());
        } catch (IOException e) {
            emitter.complete();
            this.emitters.remove(emitter);
        }
        
        emitter.onCompletion(() -> this.emitters.remove(emitter));
        emitter.onTimeout(() -> this.emitters.remove(emitter));
        return emitter;
    }

    @Scheduled(fixedRate = 5000)
    public void simulateSensorDataAndSaveToDB() {
        List<Zone> zones = zoneRepository.findAll();
        
        for (Zone zone : zones) {
            int time = random.nextInt(30);
            if (time == 0) time = 1;
            zone.setWaitTime(time);
            zoneRepository.save(zone); 
        }

        List<Zone> updatedZones = zoneRepository.findAll();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(updatedZones);
            } catch (IOException e) {
                emitter.complete();
                emitters.remove(emitter);
            }
        }
    }
}
