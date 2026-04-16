package com.example.stadiumflow.service;

import com.example.stadiumflow.domain.Zone;
import com.example.stadiumflow.repository.ZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GeminiApiServiceTest {

    private GeminiApiService geminiApiService;

    @Mock
    private ZoneRepository zoneRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        geminiApiService = new GeminiApiService(zoneRepository);
    }

    @Test
    void testInitialize_WithValidApiKey() {
        ReflectionTestUtils.setField(geminiApiService, "apiKey", "AIzaSyTest123");
        
        geminiApiService.initialize();
        
        assertTrue(geminiApiService.isAvailable());
    }

    @Test
    void testInitialize_WithPlaceholderApiKey() {
        ReflectionTestUtils.setField(geminiApiService, "apiKey", "placeholder");
        
        geminiApiService.initialize();
        
        assertFalse(geminiApiService.isAvailable());
    }

    @Test
    void testInitialize_WithNullApiKey() {
        ReflectionTestUtils.setField(geminiApiService, "apiKey", null);
        
        geminiApiService.initialize();
        
        assertFalse(geminiApiService.isAvailable());
    }

    @Test
    void testInitialize_WithEmptyApiKey() {
        ReflectionTestUtils.setField(geminiApiService, "apiKey", "");
        
        geminiApiService.initialize();
        
        assertFalse(geminiApiService.isAvailable());
    }

    @Test
    void testProcessWithGemini_NotConfigured() {
        ReflectionTestUtils.setField(geminiApiService, "isConfigured", false);
        
        String result = geminiApiService.processWithGemini("test query");
        
        assertNull(result);
    }

    @Test
    void testProcessWithGemini_WithZones() {
        ReflectionTestUtils.setField(geminiApiService, "isConfigured", false); // Will return null
        
        List<Zone> zones = Arrays.asList(
            new Zone("Zone1", "Test Zone 1", 10, 50),
            new Zone("Zone2", "Test Zone 2", 5, 25)
        );
        when(zoneRepository.findAll()).thenReturn(zones);
        
        String result = geminiApiService.processWithGemini("What is the status?");
        
        assertNull(result); // Because not configured
    }

    @Test
    void testProcessWithGemini_WithEmptyZones() {
        ReflectionTestUtils.setField(geminiApiService, "isConfigured", false);
        
        when(zoneRepository.findAll()).thenReturn(new ArrayList<>());
        
        String result = geminiApiService.processWithGemini("status");
        
        assertNull(result);
    }

    @Test
    void testGetApiKeyStatus_NotConfigured() {
        ReflectionTestUtils.setField(geminiApiService, "apiKey", null);
        
        String status = geminiApiService.getApiKeyStatus();
        
        assertEquals("Not configured", status);
    }

    @Test
    void testGetApiKeyStatus_EmptyKey() {
        ReflectionTestUtils.setField(geminiApiService, "apiKey", "");
        
        String status = geminiApiService.getApiKeyStatus();
        
        assertEquals("Not configured", status);
    }

    @Test
    void testGetApiKeyStatus_PlaceholderKey() {
        ReflectionTestUtils.setField(geminiApiService, "apiKey", "placeholder");
        
        String status = geminiApiService.getApiKeyStatus();
        
        assertEquals("Not configured", status);
    }

    @Test
    void testGetApiKeyStatus_WithValidKey() {
        ReflectionTestUtils.setField(geminiApiService, "apiKey", "AIzaSyTest1234");
        
        String status = geminiApiService.getApiKeyStatus();
        
        assertTrue(status.contains("Configured"));
        assertTrue(status.contains("1234"));
    }

    @Test
    void testGetApiKeyStatus_ShortKey() {
        ReflectionTestUtils.setField(geminiApiService, "apiKey", "ABC");
        
        String status = geminiApiService.getApiKeyStatus();
        
        assertTrue(status.contains("Configured"));
    }

    @Test
    void testIsAvailable_WhenConfigured() {
        ReflectionTestUtils.setField(geminiApiService, "isConfigured", true);
        
        assertTrue(geminiApiService.isAvailable());
    }

    @Test
    void testIsAvailable_WhenNotConfigured() {
        ReflectionTestUtils.setField(geminiApiService, "isConfigured", false);

        assertFalse(geminiApiService.isAvailable());
    }

    @Test
    void testProcessWithGemini_ExceptionDuringProcessing() {
        ReflectionTestUtils.setField(geminiApiService, "isConfigured", true);

        when(zoneRepository.findAll()).thenThrow(new RuntimeException("Database error"));

        String result = geminiApiService.processWithGemini("test");

        assertNull(result);
    }

    @Test
    void testProcessWithGemini_WithHighDensityZones() {
        ReflectionTestUtils.setField(geminiApiService, "isConfigured", false);

        List<Zone> zones = Arrays.asList(
            new Zone("Zone1", "Heavy Traffic Zone", 50, 90), // High density
            new Zone("Zone2", "Light Traffic Zone", 2, 10)   // Low density
        );
        when(zoneRepository.findAll()).thenReturn(zones);

        String result = geminiApiService.processWithGemini("status");

        assertNull(result); // Not configured
    }

    @Test
    void testInitialize_WithException() {
        GeminiApiService service = new GeminiApiService(zoneRepository);
        ReflectionTestUtils.setField(service, "apiKey", "valid-key");

        // Should not throw exception
        service.initialize();

        assertTrue(service.isAvailable());
    }
}
