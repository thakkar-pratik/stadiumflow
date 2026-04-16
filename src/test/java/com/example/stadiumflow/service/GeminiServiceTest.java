package com.example.stadiumflow.service;

import com.example.stadiumflow.domain.Zone;
import com.example.stadiumflow.dto.AiResponseDto;
import com.example.stadiumflow.repository.ZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.ArrayList;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class GeminiServiceTest {

    @Mock
    private ZoneRepository zoneRepository;

    private GeminiService geminiService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        
        // Create GeminiService with constructor parameters
        geminiService = new GeminiService(
            zoneRepository,
            "test-project-id",
            "us-central1",
            "gemini-1.5-pro",
            "test-api-key"
        );
        
        Zone gateA = new Zone("Gate_A", "Gate A", 25, 80);
        Zone gateC = new Zone("Gate_C", "Gate C", 5, 20);
        Zone section112 = new Zone("Section_112", "Section 112", 2, 10);
        Zone section120 = new Zone("Section_120", "Section 120", 15, 50);
        
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(gateA, gateC, section112, section120));
    }

    @Test
    public void testProcessQuery_SpecificZone_Busy() {
        AiResponseDto response = geminiService.processQuery("how is gate a?");
        
        assertNotNull(response);
        assertNotNull(response.getResponse());
        assertTrue(response.getResponse().contains("Gate A"));
        assertTrue(response.getResponse().contains("quite busy"));
        assertNotNull(response.getProvider());
    }

    @Test
    public void testProcessQuery_SpecificZone_Clear() {
        AiResponseDto response = geminiService.processQuery("how is gate c?");
        
        assertNotNull(response);
        assertTrue(response.getResponse().contains("Gate C"));
        assertTrue(response.getResponse().contains("flow looks great") || response.getResponse().contains("perfect time"));
    }

    @Test
    public void testProcessQuery_StatusReport() {
        AiResponseDto response = geminiService.processQuery("give me a status overview");
        
        assertNotNull(response);
        assertTrue(response.getResponse().contains("Stadium Health Report"));
        assertTrue(response.getResponse().contains("Gate A"));
    }

    @Test
    public void testProcessQuery_StatusReport_EmptyZones() {
        when(zoneRepository.findAll()).thenReturn(Arrays.asList());
        AiResponseDto response = geminiService.processQuery("give me a status overview");
        
        assertNotNull(response);
        assertTrue(response.getResponse().contains("Stadium Health Report"));
    }

    @Test
    public void testProcessQuery_DealFinder_YesDeal() {
        AiResponseDto response = geminiService.processQuery("is there a deal?");
        
        assertNotNull(response);
        assertTrue(response.getResponse().contains("Dynamic Yield discount is currently active"));
        assertTrue(response.getResponse().contains("Section 112"));
    }

    @Test
    public void testProcessQuery_DealFinder_NoDeal() {
        Zone section112 = new Zone("Section_112", "Section 112", 15, 10);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(section112));
        
        AiResponseDto response = geminiService.processQuery("give me a discount");
        
        assertNotNull(response);
        assertTrue(response.getResponse().contains("Concessions are at normal capacity."));
    }
    
    @Test
    public void testProcessQuery_DealFinder_NoStands() {
        Zone gate = new Zone("Gate_A", "Gate A", 5, 10);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(gate));
        
        AiResponseDto response = geminiService.processQuery("give me a discount");
        
        assertNotNull(response);
        assertTrue(response.getResponse().contains("Concessions are at normal capacity."));
    }

    @Test
    public void testProcessQuery_Fallback() {
        AiResponseDto response = geminiService.processQuery("hello");
        
        assertNotNull(response);
        assertTrue(response.getResponse().contains("StadiumPulse Concert Concierge"));
    }
    
    @Test
    public void testProcessQuery_ZoneMatching_VIP() {
        Zone vip = new Zone("VIP_Zone", "VIP Lounge", 10, 30);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(vip));
        
        AiResponseDto response = geminiService.processQuery("what about vip?");
        assertNotNull(response);
        assertTrue(response.getResponse().contains("VIP"));
    }
    
    @Test
    public void testProcessQuery_Food() {
        AiResponseDto response = geminiService.processQuery("where can I get food?");
        assertNotNull(response);
        assertNotNull(response.getResponse());
    }
    
    @Test
    public void testProcessQuery_Hydration() {
        Zone hydration = new Zone("Hydration_1", "Hydration Station", 3, 15);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(hydration));

        AiResponseDto response = geminiService.processQuery("hydration station status?");
        assertNotNull(response);
        assertTrue(response.getResponse().contains("Hydration"));
    }

    @Test
    public void testProcessQuery_MultipleZones_FindBestMatch() {
        Zone gate1 = new Zone("Gate_A", "Gate A", 5, 20);
        Zone gate2 = new Zone("Gate_B", "Gate B", 10, 30);
        Zone section = new Zone("Section_100", "Section 100", 8, 25);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(gate1, gate2, section));

        AiResponseDto response = geminiService.processQuery("gate b status");
        assertNotNull(response);
        assertTrue(response.getResponse().contains("Gate B"));
    }

    @Test
    public void testProcessQuery_CaseInsensitiveMatching() {
        Zone vip = new Zone("VIP_Lounge", "VIP Lounge", 2, 10);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(vip));

        AiResponseDto response = geminiService.processQuery("vip lounge");
        assertNotNull(response);
    }

    @Test
    public void testProcessQuery_PartialZoneNameMatch() {
        Zone section = new Zone("Section_112", "Section 112", 5, 20);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(section));

        AiResponseDto response = geminiService.processQuery("section 112");
        assertNotNull(response);
    }

    @Test
    public void testProcessQuery_BusyZone_RecommendAlternative() {
        Zone busyZone = new Zone("Gate_Main", "Main Gate", 25, 80);
        Zone clearZone = new Zone("Gate_Side", "Side Gate", 3, 15);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(busyZone, clearZone));

        AiResponseDto response = geminiService.processQuery("gate main");
        assertNotNull(response);
        // Response should contain information about the gate
        assertTrue(response.getResponse().length() > 0);
    }

    @Test
    public void testProcessQuery_StatusKeywords() {
        Zone zone = new Zone("Test_Zone", "Test Zone", 10, 30);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(zone));

        AiResponseDto response1 = geminiService.processQuery("status");
        AiResponseDto response2 = geminiService.processQuery("overview");
        AiResponseDto response3 = geminiService.processQuery("report");

        assertNotNull(response1);
        assertNotNull(response2);
        assertNotNull(response3);
    }

    @Test
    public void testProcessQuery_DealKeywords() {
        Zone stand = new Zone("Stand_1", "Food Stand 1", 3, 10);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(stand));

        AiResponseDto response1 = geminiService.processQuery("deal");
        AiResponseDto response2 = geminiService.processQuery("discount");
        AiResponseDto response3 = geminiService.processQuery("offer");

        assertNotNull(response1);
        assertNotNull(response2);
        assertNotNull(response3);
    }

    @Test
    public void testProcessQuery_NullQuery() {
        assertThrows(NullPointerException.class, () -> {
            geminiService.processQuery(null);
        });
    }

    @Test
    public void testProcessQuery_VeryLongQuery() {
        String longQuery = "This is a very long query ".repeat(100);
        AiResponseDto response = geminiService.processQuery(longQuery);
        assertNotNull(response);
    }

    @Test
    public void testProcessQuery_SpecialCharactersInQuery() {
        AiResponseDto response = geminiService.processQuery("!@#$%^&*()");
        assertNotNull(response);
    }

    @Test
    public void testProcessQuery_EmptyZoneList() {
        when(zoneRepository.findAll()).thenReturn(Arrays.asList());

        AiResponseDto response = geminiService.processQuery("status");
        assertNotNull(response);
        assertTrue(response.getResponse().contains("Stadium Health Report"));
    }

    @Test
    public void testProcessQuery_AllZonesBusy() {
        Zone zone1 = new Zone("Gate_1", "Gate 1", 20, 70);
        Zone zone2 = new Zone("Gate_2", "Gate 2", 25, 80);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(zone1, zone2));

        AiResponseDto response = geminiService.processQuery("gate 1");
        assertNotNull(response);
    }

    @Test
    public void testProcessQuery_ZoneWithZeroWaitTime() {
        Zone zone = new Zone("Express_Lane", "Express Lane", 0, 5);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(zone));

        AiResponseDto response = geminiService.processQuery("express lane");
        assertNotNull(response);
    }

    @Test
    public void testProcessQuery_ZoneWithHighDensity() {
        Zone zone = new Zone("Packed_Area", "Packed Area", 15, 95);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(zone));

        AiResponseDto response = geminiService.processQuery("packed area");
        assertNotNull(response);
    }

    @Test
    public void testProcessQuery_StatusKeyword_Status() {
        Zone zone = new Zone("Zone_1", "Zone 1", 10, 30);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(zone));

        AiResponseDto response = geminiService.processQuery("status");
        assertNotNull(response);
        assertTrue(response.getResponse().contains("Stadium Health Report"));
    }

    @Test
    public void testProcessQuery_StatusKeyword_Capacity() {
        Zone zone = new Zone("Zone_1", "Zone 1", 10, 30);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(zone));

        AiResponseDto response = geminiService.processQuery("what's the capacity?");
        assertNotNull(response);
        assertTrue(response.getResponse().contains("Stadium Health Report"));
    }

    @Test
    public void testProcessQuery_DealKeyword_Deal() {
        Zone stand = new Zone("Section_100", "Food Stand", 3, 10);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(stand));

        AiResponseDto response = geminiService.processQuery("any deals?");
        assertNotNull(response);
    }

    @Test
    public void testProcessQuery_DealKeyword_Cheap() {
        Zone stand = new Zone("Section_200", "Food Stand 2", 4, 15);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(stand));

        AiResponseDto response = geminiService.processQuery("where can I find cheap food?");
        assertNotNull(response);
    }

    @Test
    public void testProcessQuery_DealKeyword_Offer() {
        Zone stand = new Zone("Section_300", "Food Stand 3", 2, 8);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(stand));

        AiResponseDto response = geminiService.processQuery("any offers?");
        assertNotNull(response);
    }

    @Test
    public void testProcessQuery_ZoneMatching_North() {
        Zone north = new Zone("North_Gate", "North Gate Entrance", 8, 25);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(north));

        AiResponseDto response = geminiService.processQuery("north gate");
        assertNotNull(response);
        assertTrue(response.getResponse().contains("North"));
    }

    @Test
    public void testProcessQuery_ZoneMatching_VIPKeyword() {
        Zone vip = new Zone("VIP_Area", "VIP Lounge Area", 5, 15);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(vip));

        AiResponseDto response = geminiService.processQuery("vip area");
        assertNotNull(response);
    }

    @Test
    public void testProcessQuery_ZoneMatching_FoodKeyword() {
        Zone food = new Zone("Food_Court", "Food Court Plaza", 12, 40);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(food));

        AiResponseDto response = geminiService.processQuery("food court");
        assertNotNull(response);
    }

    @Test
    public void testProcessQuery_ZoneMatching_HydrationKeyword() {
        Zone hydration = new Zone("Hydration_Station", "Hydration Station West", 6, 20);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(hydration));

        AiResponseDto response = geminiService.processQuery("hydration");
        assertNotNull(response);
    }

    @Test
    public void testProcessQuery_ZoneMatching_GateA() {
        Zone gateA = new Zone("Gate_A", "Gate A Main", 7, 30);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(gateA));

        AiResponseDto response = geminiService.processQuery("gate a");
        assertNotNull(response);
        assertTrue(response.getResponse().contains("Gate A"));
    }

    @Test
    public void testProcessQuery_ZoneWithUnderscoreInId() {
        Zone zone = new Zone("North_Stand", "North Stand", 10, 35);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(zone));

        AiResponseDto response = geminiService.processQuery("north stand");
        assertNotNull(response);
    }

    @Test
    public void testProcessQuery_BusiestAndEmptiestZones() {
        Zone busiest = new Zone("Zone_Busy", "Busy Zone", 25, 80);
        Zone emptiest = new Zone("Zone_Empty", "Empty Zone", 2, 5);
        Zone medium = new Zone("Zone_Med", "Medium Zone", 10, 40);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(busiest, emptiest, medium));

        AiResponseDto response = geminiService.processQuery("status");
        assertNotNull(response);
        assertTrue(response.getResponse().contains("Busy Zone") || response.getResponse().contains("Empty Zone"));
    }

    @Test
    public void testProcessQuery_SingleZone_BusyTrigger() {
        Zone busy = new Zone("Main_Gate", "Main Gate", 20, 75);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(busy));

        AiResponseDto response = geminiService.processQuery("main gate");
        assertNotNull(response);
        assertTrue(response.getResponse().contains("quite busy") || response.getResponse().contains("recommend"));
    }

    @Test
    public void testProcessQuery_SingleZone_ClearFlow() {
        Zone clear = new Zone("Side_Gate", "Side Gate", 5, 15);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(clear));

        AiResponseDto response = geminiService.processQuery("side gate");
        assertNotNull(response);
        assertTrue(response.getResponse().contains("perfect time") || response.getResponse().contains("flow looks great"));
    }

    @Test
    public void testProcessQuery_DealAvailable() {
        Zone stand = new Zone("Section_112", "Food Stand Section 112", 3, 8);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(stand));

        AiResponseDto response = geminiService.processQuery("any discounts?");
        assertNotNull(response);
        assertTrue(response.getResponse().contains("Dynamic Yield discount") || response.getResponse().contains("Section 112"));
    }

    @Test
    public void testProcessQuery_NoDealAvailable_HighWaitTime() {
        Zone stand = new Zone("Section_200", "Food Stand Section 200", 10, 35);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(stand));

        AiResponseDto response = geminiService.processQuery("any discounts?");
        assertNotNull(response);
        assertTrue(response.getResponse().contains("normal capacity"));
    }

    @Test
    public void testProcessQuery_GenericGreeting() {
        Zone zone = new Zone("Zone_1", "Zone 1", 10, 30);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(zone));

        AiResponseDto response = geminiService.processQuery("hi");
        assertNotNull(response);
        assertTrue(response.getResponse().contains("StadiumPulse"));
    }

    @Test
    public void testProcessQuery_UnrecognizedQuery() {
        Zone zone = new Zone("Zone_1", "Zone 1", 10, 30);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(zone));

        AiResponseDto response = geminiService.processQuery("random unrecognized text xyz");
        assertNotNull(response);
        assertTrue(response.getResponse().contains("StadiumPulse"));
    }

    @Test
    public void testProcessQuery_ZoneWaitTimeExactly15Minutes() {
        Zone zone = new Zone("Gate_Test", "Test Gate", 15, 50);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(zone));

        AiResponseDto response = geminiService.processQuery("gate test");
        assertNotNull(response);
        assertTrue(response.getResponse().contains("flow looks great") || response.getResponse().contains("perfect time"));
    }

    @Test
    public void testProcessQuery_ZoneWaitTimeAt16Minutes() {
        Zone zone = new Zone("Gate_Test2", "Test Gate 2", 16, 55);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(zone));

        AiResponseDto response = geminiService.processQuery("gate test2");
        assertNotNull(response);
        assertTrue(response.getResponse().contains("quite busy"));
    }

    @Test
    public void testProcessQuery_DealWithStandAtExactly5Minutes() {
        Zone stand = new Zone("Section_200", "Food Stand 200", 5, 25);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(stand));

        AiResponseDto response = geminiService.processQuery("deal");
        assertNotNull(response);
        assertTrue(response.getResponse().contains("normal capacity"));
    }

    @Test
    public void testProcessQuery_DealWithStandAt4Minutes() {
        Zone stand = new Zone("Section_300", "Food Stand 300", 4, 20);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(stand));

        AiResponseDto response = geminiService.processQuery("deal");
        assertNotNull(response);
        assertTrue(response.getResponse().contains("Dynamic Yield") || response.getResponse().contains("low wait"));
    }

    @Test
    public void testProcessQuery_HowIsKeyword() {
        Zone zone = new Zone("Gate_C", "Gate C", 9, 35);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(zone));

        AiResponseDto response = geminiService.processQuery("how is everything?");
        assertNotNull(response);
        assertTrue(response.getResponse().contains("Stadium Health Report"));
    }

    @Test
    public void testProcessQuery_ZoneMatch_NorthKeyword() {
        Zone northZone = new Zone("North_1", "North Stand", 8, 30);
        Zone southZone = new Zone("South_1", "South Stand", 12, 45);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(northZone, southZone));

        AiResponseDto response = geminiService.processQuery("what about north?");
        assertNotNull(response);
        assertTrue(response.getResponse().toLowerCase().contains("north"));
    }

    @Test
    public void testProcessQuery_ZoneMatch_VIPKeywordInName() {
        Zone vipZone = new Zone("VIP_1", "VIP Lounge", 4, 18);
        Zone normalZone = new Zone("Gate_1", "Normal Gate", 11, 42);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(vipZone, normalZone));

        AiResponseDto response = geminiService.processQuery("tell me about vip");
        assertNotNull(response);
        assertTrue(response.getResponse().toLowerCase().contains("vip"));
    }

    @Test
    public void testProcessQuery_ZoneMatch_FoodKeywordInName() {
        Zone foodZone = new Zone("Food_1", "Food Court", 6, 25);
        Zone gateZone = new Zone("Gate_2", "Main Gate", 13, 48);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(foodZone, gateZone));

        AiResponseDto response = geminiService.processQuery("food court status");
        assertNotNull(response);
        assertTrue(response.getResponse().toLowerCase().contains("food"));
    }

    @Test
    public void testProcessQuery_ZoneMatch_HydrationKeywordInName() {
        Zone hydrationZone = new Zone("Hydration_1", "Hydration Station", 3, 14);
        Zone otherZone = new Zone("Gate_3", "Gate 3", 10, 38);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(hydrationZone, otherZone));

        AiResponseDto response = geminiService.processQuery("hydration station");
        assertNotNull(response);
        assertTrue(response.getResponse().toLowerCase().contains("hydration"));
    }

    @Test
    public void testProcessQuery_ZoneMatch_GateAKeywordInName() {
        Zone gateA = new Zone("Gate_A", "Gate A Main", 7, 28);
        Zone gateB = new Zone("Gate_B", "Gate B Side", 14, 52);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(gateA, gateB));

        AiResponseDto response = geminiService.processQuery("gate a status");
        assertNotNull(response);
        assertTrue(response.getResponse().toLowerCase().contains("gate a"));
    }

    @Test
    public void testProcessQuery_ZoneMatch_IdWithUnderscore() {
        Zone zone = new Zone("Main_Entrance", "Main Entrance Hall", 9, 34);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(zone));

        AiResponseDto response = geminiService.processQuery("main entrance");
        assertNotNull(response);
        assertTrue(response.getResponse().toLowerCase().contains("main entrance"));
    }

    @Test
    public void testProcessQuery_ZoneMatch_ByIdLowercase() {
        Zone zone = new Zone("Gate_C", "Gate C", 11, 41);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(zone));

        AiResponseDto response = geminiService.processQuery("gate_c");
        assertNotNull(response);
    }

    @Test
    public void testProcessQuery_NoZoneMatch_StatusWithBusiestNull() {
        when(zoneRepository.findAll()).thenReturn(Arrays.asList());

        AiResponseDto response = geminiService.processQuery("status");
        assertNotNull(response);
        assertTrue(response.getResponse().contains("Stadium Health Report"));
    }

    @Test
    public void testProcessQuery_StatusWithBusiestAndEmptiestFound() {
        Zone zone1 = new Zone("Z1", "Zone 1", 5, 20);
        Zone zone2 = new Zone("Z2", "Zone 2", 15, 60);
        Zone zone3 = new Zone("Z3", "Zone 3", 10, 40);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(zone1, zone2, zone3));

        AiResponseDto response = geminiService.processQuery("status");
        assertNotNull(response);
        assertTrue(response.getResponse().contains("Zone 2")); // busiest
        assertTrue(response.getResponse().contains("Zone 1")); // emptiest
    }

    @Test
    public void testProcessQuery_DealWith_SectionStandWaitTimeLessThan5() {
        Zone stand = new Zone("Section_100", "Food Stand 100", 4, 18);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(stand));

        AiResponseDto response = geminiService.processQuery("deal");
        assertNotNull(response);
        assertTrue(response.getResponse().contains("Dynamic Yield") || response.getResponse().contains("low wait"));
    }

    @Test
    public void testProcessQuery_DealWith_SectionStandWaitTime5OrMore() {
        Zone stand = new Zone("Section_200", "Food Stand 200", 5, 25);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(stand));

        AiResponseDto response = geminiService.processQuery("deal");
        assertNotNull(response);
        assertTrue(response.getResponse().contains("normal capacity"));
    }

    @Test
    public void testProcessQuery_DealWith_NoSectionStands() {
        Zone gate = new Zone("Gate_Main", "Main Gate", 8, 32);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(gate));

        AiResponseDto response = geminiService.processQuery("cheap");
        assertNotNull(response);
        assertTrue(response.getResponse().contains("normal capacity"));
    }

    @Test
    public void testProcessQuery_DealWith_EmptyStandsList() {
        when(zoneRepository.findAll()).thenReturn(Arrays.asList());

        AiResponseDto response = geminiService.processQuery("discount");
        assertNotNull(response);
        assertTrue(response.getResponse().contains("normal capacity"));
    }

    @Test
    public void testProcessQuery_FallbackMessage() {
        Zone zone = new Zone("Test", "Test", 10, 30);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(zone));

        AiResponseDto response = geminiService.processQuery("xyz random text");
        assertNotNull(response);
        assertTrue(response.getResponse().contains("StadiumPulse"));
    }

    @Test
    public void testProcessQuery_OverviewKeyword() {
        Zone zone = new Zone("Test", "Test", 8, 28);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(zone));

        AiResponseDto response = geminiService.processQuery("overview");
        assertNotNull(response);
        assertTrue(response.getResponse().contains("Stadium Health Report"));
    }

    @Test
    public void testProcessQuery_CapacityKeyword() {
        Zone zone = new Zone("Test", "Test", 12, 44);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(zone));

        AiResponseDto response = geminiService.processQuery("capacity");
        assertNotNull(response);
        assertTrue(response.getResponse().contains("Stadium Health Report"));
    }

    @Test
    public void testProcessQuery_WithEmptyStringQuery() {
        Zone zone = new Zone("Test", "Test", 10, 30);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(zone));

        AiResponseDto response = geminiService.processQuery("");
        assertNotNull(response);
        assertTrue(response.getResponse().contains("StadiumPulse"));
    }

    @Test
    public void testProcessQuery_WithWhitespaceOnly() {
        Zone zone = new Zone("Test", "Test", 10, 30);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(zone));

        AiResponseDto response = geminiService.processQuery("   ");
        assertNotNull(response);
    }

    @Test
    public void testProcessQuery_WithVeryLongQuery() {
        String longQuery = "What is the status of " + "zone ".repeat(1000) + "?";
        Zone zone = new Zone("Test", "Test", 10, 30);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(zone));

        AiResponseDto response = geminiService.processQuery(longQuery);
        assertNotNull(response);
    }

    @Test
    public void testProcessQuery_WithSpecialCharacters() {
        Zone zone = new Zone("Test", "Test", 10, 30);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(zone));

        AiResponseDto response = geminiService.processQuery("!@#$%^&*()_+{}|:<>?");
        assertNotNull(response);
    }

    @Test
    public void testProcessQuery_WithNumbers() {
        Zone zone = new Zone("Test", "Test", 10, 30);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(zone));

        AiResponseDto response = geminiService.processQuery("12345");
        assertNotNull(response);
    }

    @Test
    public void testProcessQuery_MixedCaseStatus() {
        Zone zone = new Zone("Test", "Test", 10, 30);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(zone));

        AiResponseDto response = geminiService.processQuery("StAtUs");
        assertNotNull(response);
        assertTrue(response.getResponse().contains("Stadium Health Report"));
    }

    @Test
    public void testProcessQuery_MixedCaseDeal() {
        Zone stand = new Zone("Section_100", "Food Stand", 3, 10);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(stand));

        AiResponseDto response = geminiService.processQuery("DeAl");
        assertNotNull(response);
    }

    @Test
    public void testProcessQuery_StatusWithSingleZone() {
        Zone zone = new Zone("Only_Zone", "Only Zone", 10, 35);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(zone));

        AiResponseDto response = geminiService.processQuery("status");
        assertNotNull(response);
        assertTrue(response.getResponse().contains("Only Zone"));
    }

    @Test
    public void testProcessQuery_AllZonesSameWaitTime() {
        Zone z1 = new Zone("Z1", "Zone 1", 10, 30);
        Zone z2 = new Zone("Z2", "Zone 2", 10, 30);
        Zone z3 = new Zone("Z3", "Zone 3", 10, 30);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(z1, z2, z3));

        AiResponseDto response = geminiService.processQuery("status");
        assertNotNull(response);
    }

    @Test
    public void testProcessQuery_DealWithMultipleSections() {
        Zone s1 = new Zone("Section_100", "Stand 100", 4, 18);
        Zone s2 = new Zone("Section_200", "Stand 200", 3, 15);
        Zone s3 = new Zone("Section_300", "Stand 300", 6, 22);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(s1, s2, s3));

        AiResponseDto response = geminiService.processQuery("deal");
        assertNotNull(response);
    }

    @Test
    public void testProcessQuery_ZoneMatchWithPartialName() {
        Zone zone = new Zone("North_VIP_Food_Court", "North VIP Food Court", 7, 28);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(zone));

        AiResponseDto response = geminiService.processQuery("vip");
        assertNotNull(response);
    }

    @Test
    public void testProcessQuery_WithEmptyZoneDatabase() {
        when(zoneRepository.findAll()).thenReturn(new ArrayList<>());
        AiResponseDto response = geminiService.processQuery("status");
        assertNotNull(response);
        assertNotNull(response.getProvider());
    }

    @Test
    public void testProcessQuery_SpecialCharactersHandling() {
        AiResponseDto response = geminiService.processQuery("!@#$%^&*()");
        assertNotNull(response);
        assertNotNull(response.getProvider());
    }

    @Test
    public void testProcessQuery_ValidQueryWithValidResponse() {
        AiResponseDto response = geminiService.processQuery("What's the stadium status?");
        assertNotNull(response);
        assertNotNull(response.getProvider());
        assertNotNull(response.getResponse());
    }
}


