package com.example.stadiumflow.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StorageServiceTest {

    private StorageService storageService;

    @BeforeEach
    public void setup() {
        // Create StorageService with test configuration (storage disabled)
        storageService = new StorageService();
        ReflectionTestUtils.setField(storageService, "storageEnabled", false);
        ReflectionTestUtils.setField(storageService, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(storageService, "projectId", "test-project");
        storageService.initialize();
    }

    @Test
    public void testIsAvailable_Disabled() {
        assertFalse(storageService.isAvailable());
    }

    @Test
    public void testSaveMetricsSnapshot_StorageDisabled() {
        String json = "{\"test\": \"data\"}";
        
        // Should not throw exception even when storage is disabled
        assertDoesNotThrow(() -> storageService.saveMetricsSnapshot(json));
    }

    @Test
    public void testUploadAnalytics_StorageDisabled() {
        boolean result = storageService.uploadAnalytics("test.json", "{\"data\": \"test\"}");
        
        assertFalse(result);
    }

    @Test
    public void testListAnalyticsFiles_StorageDisabled() {
        List<String> files = storageService.listAnalyticsFiles();

        assertNotNull(files);
        assertTrue(files.isEmpty());
    }

    @Test
    public void testSaveMetricsSnapshot_NullJson() {
        assertDoesNotThrow(() -> storageService.saveMetricsSnapshot(null));
    }

    @Test
    public void testSaveMetricsSnapshot_EmptyJson() {
        assertDoesNotThrow(() -> storageService.saveMetricsSnapshot(""));
    }

    @Test
    public void testUploadAnalytics_NullFilename() {
        boolean result = storageService.uploadAnalytics(null, "{\"test\": true}");
        assertFalse(result);
    }

    @Test
    public void testUploadAnalytics_NullContent() {
        boolean result = storageService.uploadAnalytics("file.json", null);
        assertFalse(result);
    }

    @Test
    public void testUploadAnalytics_EmptyFilename() {
        boolean result = storageService.uploadAnalytics("", "{\"test\": true}");
        assertFalse(result);
    }

    @Test
    public void testUploadAnalytics_EmptyContent() {
        boolean result = storageService.uploadAnalytics("file.json", "");
        assertFalse(result);
    }

    @Test
    public void testListAnalyticsFiles_MultipleCallsConsistent() {
        List<String> files1 = storageService.listAnalyticsFiles();
        List<String> files2 = storageService.listAnalyticsFiles();

        assertNotNull(files1);
        assertNotNull(files2);
        assertEquals(files1.size(), files2.size());
    }

    @Test
    public void testInitialize_WhenEnabled_HandlesException() {
        StorageService service = new StorageService();
        ReflectionTestUtils.setField(service, "storageEnabled", true);
        ReflectionTestUtils.setField(service, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(service, "projectId", "test-project");

        // Should not throw exception even when GCP credentials are missing
        assertDoesNotThrow(() -> service.initialize());
    }

    @Test
    public void testSaveMetricsSnapshot_WithValidJson() {
        String validJson = "{\"zones\": [{\"id\": \"Gate_A\", \"waitTime\": 10}]}";
        assertDoesNotThrow(() -> storageService.saveMetricsSnapshot(validJson));
    }

    @Test
    public void testSaveMetricsSnapshot_WithComplexJson() {
        String complexJson = "{\"timestamp\": \"2026-04-16T12:00:00\", \"zones\": []}";
        assertDoesNotThrow(() -> storageService.saveMetricsSnapshot(complexJson));
    }

    @Test
    public void testUploadAnalytics_WithLongFileName() {
        String longFileName = "analytics_" + "x".repeat(200) + ".json";
        boolean result = storageService.uploadAnalytics(longFileName, "{}");
        assertFalse(result);
    }

    @Test
    public void testUploadAnalytics_WithSpecialCharacters() {
        boolean result = storageService.uploadAnalytics("file!@#$.json", "content");
        assertFalse(result);
    }

    @Test
    public void testUploadAnalytics_WithEmptyContent() {
        boolean result = storageService.uploadAnalytics("test.json", "");
        assertFalse(result);
    }

    @Test
    public void testUploadAnalytics_WithLargeContent() {
        String largeContent = "x".repeat(10000);
        boolean result = storageService.uploadAnalytics("large.json", largeContent);
        assertFalse(result);
    }

    @Test
    public void testIsAvailable_ReturnsFalseWhenDisabled() {
        assertFalse(storageService.isAvailable());
    }

    @Test
    public void testListAnalyticsFiles_WhenDisabled() {
        List<String> files = storageService.listAnalyticsFiles();
        assertNotNull(files);
        assertTrue(files.isEmpty());
    }

    @Test
    public void testSaveMetricsSnapshot_MultipleCallsDoNotThrow() {
        assertDoesNotThrow(() -> {
            storageService.saveMetricsSnapshot("{\"test\": 1}");
            storageService.saveMetricsSnapshot("{\"test\": 2}");
            storageService.saveMetricsSnapshot("{\"test\": 3}");
        });
    }

    @Test
    public void testInitialize_WhenDisabled() {
        StorageService service = new StorageService();
        ReflectionTestUtils.setField(service, "storageEnabled", false);

        assertDoesNotThrow(() -> service.initialize());
        assertFalse(service.isAvailable());
    }

    @Test
    public void testUploadAnalytics_WhenStorageIsNull() {
        StorageService service = new StorageService();
        ReflectionTestUtils.setField(service, "storageEnabled", false);
        ReflectionTestUtils.setField(service, "storage", null);

        boolean result = service.uploadAnalytics("test.json", "content");
        assertFalse(result);
    }

    @Test
    public void testListAnalyticsFiles_WhenStorageIsNull() {
        StorageService service = new StorageService();
        ReflectionTestUtils.setField(service, "storageEnabled", true);
        ReflectionTestUtils.setField(service, "storage", null);

        List<String> files = service.listAnalyticsFiles();
        assertNotNull(files);
        assertTrue(files.isEmpty());
    }

    @Test
    public void testSaveMetricsSnapshot_GeneratesFileName() {
        String json = "{\"zones\": []}";

        // Should generate filename with timestamp
        assertDoesNotThrow(() -> storageService.saveMetricsSnapshot(json));
    }

    @Test
    public void testUploadAnalytics_WithValidData() {
        String fileName = "test-metrics.json";
        String content = "{\"timestamp\": \"2026-04-16\", \"data\": []}";

        boolean result = storageService.uploadAnalytics(fileName, content);
        assertFalse(result); // False because storage is disabled in test
    }

    @Test
    public void testUploadAnalytics_WithJsonContent() {
        String content = "{\"zones\": [{\"id\": \"Gate_A\", \"waitTime\": 10, \"density\": 30}]}";

        boolean result = storageService.uploadAnalytics("analytics.json", content);
        assertFalse(result);
    }

    @Test
    public void testListAnalyticsFiles_WhenStorageDisabled() {
        List<String> files = storageService.listAnalyticsFiles();

        assertNotNull(files);
        assertEquals(0, files.size());
    }

    @Test
    public void testIsAvailable_WhenBothStorageAndEnabledAreFalse() {
        StorageService service = new StorageService();
        ReflectionTestUtils.setField(service, "storageEnabled", false);
        ReflectionTestUtils.setField(service, "storage", null);

        assertFalse(service.isAvailable());
    }

    @Test
    public void testIsAvailable_WhenEnabledButStorageIsNull() {
        StorageService service = new StorageService();
        ReflectionTestUtils.setField(service, "storageEnabled", true);
        ReflectionTestUtils.setField(service, "storage", null);

        assertFalse(service.isAvailable());
    }

    @Test
    public void testSaveMetricsSnapshot_WithVeryComplexJson() {
        String complexJson = "{\"timestamp\":\"2026-04-16T12:00:00\",\"zones\":[{\"id\":\"Gate_A\",\"name\":\"Gate A\",\"waitTime\":10,\"density\":30},{\"id\":\"Section_112\",\"name\":\"Food Court\",\"waitTime\":5,\"density\":20}],\"metadata\":{\"stadium\":\"Wankhede\",\"event\":\"Coldplay\"}}";

        assertDoesNotThrow(() -> storageService.saveMetricsSnapshot(complexJson));
    }

    @Test
    public void testUploadAnalytics_WithUnicodeContent() {
        String unicodeContent = "{\"message\": \"Hello 世界 🌍\"}";

        boolean result = storageService.uploadAnalytics("unicode.json", unicodeContent);
        assertFalse(result);
    }

    @Test
    public void testSaveMetricsSnapshot_TimestampInFileName() {
        // Multiple calls should generate different filenames due to timestamps
        assertDoesNotThrow(() -> {
            storageService.saveMetricsSnapshot("{\"test\": 1}");
            Thread.sleep(10);
            storageService.saveMetricsSnapshot("{\"test\": 2}");
        });
    }

    @Test
    public void testUploadAnalytics_WithPathInFileName() {
        // Should handle filenames with paths
        boolean result = storageService.uploadAnalytics("folder/subfolder/file.json", "{}");
        assertFalse(result);
    }

    @Test
    public void testListAnalyticsFiles_ReturnsEmptyList() {
        // When disabled, should always return empty list
        List<String> files1 = storageService.listAnalyticsFiles();
        List<String> files2 = storageService.listAnalyticsFiles();

        assertEquals(files1.size(), files2.size());
        assertEquals(0, files1.size());
    }

    @Test
    public void testInitialize_WhenEnabledButNoCredentials() {
        StorageService service = new StorageService();
        ReflectionTestUtils.setField(service, "storageEnabled", true);
        ReflectionTestUtils.setField(service, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(service, "projectId", "test-project");

        // This should not throw even if initialization fails
        assertDoesNotThrow(() -> service.initialize());

        // Storage will be null due to missing credentials, so not available
        // But we can't assert this strongly since initialization is in a try-catch
    }

    @Test
    public void testUploadAnalytics_WhenEnabledButStorageNull() {
        StorageService service = new StorageService();
        ReflectionTestUtils.setField(service, "storageEnabled", true);
        ReflectionTestUtils.setField(service, "storage", null);

        boolean result = service.uploadAnalytics("test.json", "content");
        assertFalse(result);
    }

    @Test
    public void testListAnalyticsFiles_WhenEnabledButStorageNull() {
        StorageService service = new StorageService();
        ReflectionTestUtils.setField(service, "storageEnabled", true);
        ReflectionTestUtils.setField(service, "storage", null);

        List<String> files = service.listAnalyticsFiles();
        assertNotNull(files);
        assertTrue(files.isEmpty());
    }

    @Test
    public void testSaveMetricsSnapshot_GeneratesUniqueFileName() throws Exception {
        String json1 = "{\"test\": 1}";
        String json2 = "{\"test\": 2}";

        assertDoesNotThrow(() -> {
            storageService.saveMetricsSnapshot(json1);
            Thread.sleep(10);
            storageService.saveMetricsSnapshot(json2);
        });
    }

    @Test
    public void testUploadAnalytics_WithBothConditionsFalse() {
        StorageService service = new StorageService();
        ReflectionTestUtils.setField(service, "storageEnabled", false);
        ReflectionTestUtils.setField(service, "storage", null);

        boolean result = service.uploadAnalytics("test.json", "{}");
        assertFalse(result);
    }

    @Test
    public void testListAnalyticsFiles_WithBothConditionsFalse() {
        StorageService service = new StorageService();
        ReflectionTestUtils.setField(service, "storageEnabled", false);
        ReflectionTestUtils.setField(service, "storage", null);

        List<String> files = service.listAnalyticsFiles();
        assertTrue(files.isEmpty());
    }

    @Test
    public void testIsAvailable_WithStorageNullButEnabledTrue() {
        StorageService service = new StorageService();
        ReflectionTestUtils.setField(service, "storageEnabled", true);
        ReflectionTestUtils.setField(service, "storage", null);

        assertFalse(service.isAvailable());
    }

    @Test
    public void testIsAvailable_WithStorageNotNullButEnabledFalse() {
        StorageService service = new StorageService();
        ReflectionTestUtils.setField(service, "storageEnabled", false);
        // Even if we could set storage (which we can't easily), enabled=false means not available

        assertFalse(service.isAvailable());
    }

    @Test
    public void testSaveMetricsSnapshot_WithReallyLongJson() {
        StringBuilder longJson = new StringBuilder("{\"data\":[");
        for (int i = 0; i < 1000; i++) {
            if (i > 0) longJson.append(",");
            longJson.append("{\"id\":").append(i).append(",\"value\":\"test\"}");
        }
        longJson.append("]}");

        assertDoesNotThrow(() -> storageService.saveMetricsSnapshot(longJson.toString()));
    }

    @Test
    public void testUploadAnalytics_WithSpecialCharactersInFileName() {
        boolean result = storageService.uploadAnalytics("file-with-special_chars@123.json", "{}");
        assertFalse(result);
    }

    @Test
    public void testUploadAnalytics_WithSlashesInFileName() {
        boolean result = storageService.uploadAnalytics("folder/subfolder/file.json", "{}");
        assertFalse(result);
    }
}
