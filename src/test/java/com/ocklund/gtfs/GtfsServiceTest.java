package com.ocklund.gtfs;

import com.ocklund.gtfs.configuration.TimeProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static com.ocklund.gtfs.GtfsService.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GtfsServiceTest {

    @Mock
    private TimeProvider timeProvider;
    @InjectMocks
    private GtfsService gtfsService;
    
    @Test
    void getStopReports_shouldReturnCorrectReports() {
        Map<String, List<StopTime>> stopTimesMap = new HashMap<>();
        String[] stopIds = {
                STOP_ID_TRAM_FROM_LILJEHOLMEN,
                STOP_ID_TRAM_TO_LILJEHOLMEN,
                STOP_ID_BUS_TO_OSTBERGAHOJDEN,
                STOP_ID_BUS_TO_LILJEHOLMEN
        };
        for (String stopId : stopIds) {
            List<StopTime> stopTimes = new ArrayList<>();
            stopTimes.add(new StopTime("trip" + stopId, stopId, "10:30:00", "10:35:00", "Destination " + stopId));
            stopTimesMap.put(stopId, stopTimes);
        }
        Map<String, Trip> tripsMap = new HashMap<>();
        for (String stopId : stopIds) {
            String tripId = "trip" + stopId;
            tripsMap.put(tripId, new Trip(tripId, "service" + stopId));
        }
        Set<String> activeServiceIds = new HashSet<>();
        for (String stopId : stopIds) {
            activeServiceIds.add("service" + stopId);
        }
        gtfsService.setStopTimesMap(stopTimesMap);
        gtfsService.setTripsMap(tripsMap);
        gtfsService.setActiveServiceIds(activeServiceIds);
        
        List<String> reports = gtfsService.getStopReports();
        
        assertNotNull(reports, "Reports should not be null");
        assertEquals(4, reports.size(), "There should be 4 reports (one for each target stop)");
        for (String report : reports) {
            assertNotNull(report, "Report should not be null");
            assertFalse(report.isEmpty(), "Report should not be empty");
        }
    }
    
    @Test
    void parseGtfsTime_shouldReturnCorrectDateTimeOrThrowWhenInvalidFormat() {
        // Setup a fixed current date for testing
        LocalDateTime currentDateTime = LocalDateTime.of(2025, 8, 6, 12, 0);
        when(timeProvider.now(any(ZoneId.class))).thenReturn(currentDateTime);
        
        // Test normal time
        LocalDateTime result1 = gtfsService.parseGtfsTime("14:30:00");
        assertNotNull(result1, "Parsed time should not be null");
        assertEquals(14, result1.getHour(), "Hour should be 14");
        assertEquals(30, result1.getMinute(), "Minute should be 30");
        assertEquals(2025, result1.getYear(), "Year should be 2025");
        assertEquals(8, result1.getMonthValue(), "Month should be 8");
        assertEquals(6, result1.getDayOfMonth(), "Day should be 6");
        
        // Test time exceeding 24 hours
        LocalDateTime result2 = gtfsService.parseGtfsTime("25:45:00");
        assertNotNull(result2, "Parsed time should not be null");
        assertEquals(1, result2.getHour(), "Hour should be 1 (25 % 24)");
        assertEquals(45, result2.getMinute(), "Minute should be 45");
        assertEquals(2025, result2.getYear(), "Year should be 2025");
        assertEquals(8, result2.getMonthValue(), "Month should be 8");
        assertEquals(7, result2.getDayOfMonth(), "Day should be 7 (next day)");
        
        // Test invalid format
        assertThrows(IllegalArgumentException.class, () ->
                gtfsService.parseGtfsTime("invalid"), "Should throw exception for invalid time format");
    }
    
    @Test
    void isOutsideTimeWindow_shouldReturnCorrectResult() {
        LocalDateTime testTime = LocalDateTime.of(2025, 8, 6, 15, 33);
        LocalDateTime pastTime = testTime.minusHours(1);
        LocalDateTime futureTime = testTime.plusHours(1);
        
        // Mock the TimeProvider to return the test time
        when(timeProvider.now(any(ZoneId.class))).thenReturn(testTime);
        
        // Test with before time (should be outside the time window)
        boolean pastResult = gtfsService.isOutsideTimeWindow(pastTime);
        assertTrue(pastResult, "Past time should be outside time window");
        
        // Test with future time that's outside the window (more than 15 minutes ahead)
        boolean futureResult = gtfsService.isOutsideTimeWindow(futureTime);
        assertTrue(futureResult, "Future time (1 hour ahead) should be outside time window");
        
        // Test with a time that's inside the window (less than 15 minutes ahead)
        LocalDateTime nearFutureTime = testTime.plusMinutes(10);
        boolean nearFutureResult = gtfsService.isOutsideTimeWindow(nearFutureTime);
        assertFalse(nearFutureResult, "Near future time (10 minutes ahead) should be inside time window");
    }
}
