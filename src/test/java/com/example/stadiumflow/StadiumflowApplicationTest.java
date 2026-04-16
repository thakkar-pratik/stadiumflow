package com.example.stadiumflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the main application class
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "firebase.project-id=test-project",
    "gcp.project-id=test-project",
    "gcp.location=us-central1",
    "gcp.model-name=gemini-pro",
    "gemini.api-key=test-key"
})
public class StadiumflowApplicationTest {

    @Test
    public void contextLoads() {
        // Test that the Spring context loads successfully
        assertDoesNotThrow(() -> {
            // Context loaded successfully if we get here
        });
    }

    @Test
    public void testMainMethod() {
        // Test that main method can be called without error
        assertDoesNotThrow(() -> {
            // Don't actually run the full application, just verify the method exists
            StadiumflowApplication.class.getMethod("main", String[].class);
        });
    }

    @Test
    public void testApplicationStartup() {
        // Verify the application class exists and is properly annotated
        assertNotNull(StadiumflowApplication.class.getAnnotation(
            org.springframework.boot.autoconfigure.SpringBootApplication.class
        ));
    }

    @Test
    public void testSpringBootConfiguration() {
        // Verify it's a Spring Boot application
        assertTrue(StadiumflowApplication.class.isAnnotationPresent(
            org.springframework.boot.autoconfigure.SpringBootApplication.class
        ));
    }
}
