
package com.ocklund.gtfs;

import com.ocklund.gtfs.configuration.TimeProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class GtfsService {

    // Selected target stop ids from file routes.txt:
    // stop_id,stop_name,stop_lat,stop_lon,location_type,parent_station,platform_code
    // 9022001013905002,Sjövikstorget,59.307482,18.028621,0,9021001013905000,
    static final String STOP_ID_BUS_TO_LILJEHOLMEN = "9022001013905002";
    // 9022001013905001,Sjövikstorget,59.307300,18.028835,0,9021001013905000,
    static final String STOP_ID_BUS_TO_OSTBERGAHOJDEN = "9022001013905001";
    // 9022001004513001,Årstadal,59.305943,18.025454,0,9021001004513000,1
    static final String STOP_ID_TRAM_FROM_LILJEHOLMEN = "9022001004513001";
    // 9022001004513002,Årstadal,59.305707,18.025596,0,9021001004513000,2
    static final String STOP_ID_TRAM_TO_LILJEHOLMEN = "9022001004513002";

    private final TimeProvider timeProvider;

    // Using LinkedHashSet to maintain insertion order for consistent quadrant display
    private static final Set<String> TARGET_STOP_IDS = new LinkedHashSet<>(
            Arrays.asList(
                    STOP_ID_TRAM_FROM_LILJEHOLMEN,    // Top-left: Solna station
                    STOP_ID_TRAM_TO_LILJEHOLMEN,      // Top-right: Sickla
                    STOP_ID_BUS_TO_OSTBERGAHOJDEN,       // Bottom-left: Östbergahöjden
                    STOP_ID_BUS_TO_LILJEHOLMEN           // Bottom-right: Liljeholmen
            )
    );
    private Map<String, Trip> tripsMap = new HashMap<>();
    // Map to store scheduled stop times: key is stopId, value is a list of StopTime objects for that stop
    private Map<String, List<StopTime>> stopTimesMap = new HashMap<>();
    // Map to store child-to-parent stop relationships: key is child stopId, value is parent stopId
    private final Map<String, String> childToParentMap = new HashMap<>();
    // Set to store active service IDs for the current date
    private Set<String> activeServiceIds = new HashSet<>();

    public GtfsService(TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
    }

    /**
     * Package-private setter for tripsMap (used for testing)
     */
    void setTripsMap(Map<String, Trip> tripsMap) {
        this.tripsMap = tripsMap;
    }
    
    /**
     * Package-private setter for stopTimesMap (used for testing)
     */
    void setStopTimesMap(Map<String, List<StopTime>> stopTimesMap) {
        this.stopTimesMap = stopTimesMap;
    }
    
    /**
     * Package-private setter for activeServiceIds (used for testing)
     */
    void setActiveServiceIds(Set<String> activeServiceIds) {
        this.activeServiceIds = activeServiceIds;
    }
    
    private static final ZoneId STOCKHOLM_ZONE = ZoneId.of("Europe/Stockholm");
    private static final int TIME_WINDOW_MINUTES = 15;

    @PostConstruct
    public void init() {
        loadStops();
        loadTrips();
        loadStopTimes();
        loadCalendarData();
    }

    public List<String> getStopReports() {
        List<String> reports = new ArrayList<>();
        for (String stopId : TARGET_STOP_IDS) {
            StringBuilder sb = new StringBuilder();

            // Display scheduled departures
            List<StopTime> scheduledTimes = stopTimesMap.get(stopId);
            if (scheduledTimes != null && !scheduledTimes.isEmpty()) {
                // Sort by arrival time
                scheduledTimes.sort(Comparator.comparing(StopTime::arrivalTime));
                
                boolean hasUpcomingDepartures = false;
                
                // Map to store one departure per minute (key is minute, value is the departure time)
                Map<String, String> minuteToDepartureMap = new HashMap<>();
                
                for (StopTime stopTime : scheduledTimes) {
                    // Check if this departure is within the time window
                    try {
                        LocalDateTime departureTime = parseGtfsTime(stopTime.departureTime());
                        if (isOutsideTimeWindow(departureTime)) {
                            continue; // Skip departures outside the time window
                        }
                        
                        String tripId = stopTime.tripId();
                        Trip trip = tripsMap.get(tripId);
                        if (trip != null) {
                            // Get the service ID for this trip
                            String serviceId = trip.serviceId();
                            
                            // Check if this service is active on the current date
                            if (activeServiceIds.contains(serviceId)) {
                                // Extract minute from departure time (HH:MM format)
                                String departureMinute = stopTime.departureTime().substring(0, 5);
                                
                                // Only add this departure if we don't already have one for this minute
                                if (!minuteToDepartureMap.containsKey(departureMinute)) {
                                    // Store both departure time and stop headsign
                                    String stopHeadsign = stopTime.stopHeadsign() != null ? stopTime.stopHeadsign() : "N/A";
                                    minuteToDepartureMap.put(departureMinute, stopTime.departureTime() + "|" + stopHeadsign);
                                    hasUpcomingDepartures = true;
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing time: " + stopTime.departureTime() + " - " + e.getMessage());
                    }
                }
                
                // Display the filtered departures
                List<String> sortedMinutes = new ArrayList<>(minuteToDepartureMap.keySet());
                Collections.sort(sortedMinutes);
                
                for (String minute : sortedMinutes) {
                    String combinedValue = minuteToDepartureMap.get(minute);
                    String[] parts = combinedValue.split("\\|", 2);
                    String departureTime = parts[0].substring(0, 5); // Only use hours and minutes (HH:MM)
                    String destination = parts.length > 1 ? parts[1] : "";
                    sb.append(departureTime).append(" → ").append(destination).append("<br>");
                }
                
                if (!hasUpcomingDepartures) {
                    sb.append("Inga avgångar de närmaste ").append(TIME_WINDOW_MINUTES).append(" minuterna, enligt tidtabell");
                }
            } else {
                sb.append("Inga avgångar de närmaste ").append(TIME_WINDOW_MINUTES).append(" minuterna, enligt tidtabell");
            }
            
            reports.add(sb.toString());

        }
        return reports;
    }

    void loadStops() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("gtfs/stops.txt").getInputStream()))) {
            reader.readLine(); // header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] p = line.split(",", -1);
                if (p.length >= 6) {  // Make sure we have enough fields including parent_station
                    String stopId = p[0];
                    String parentStation = p[5];
                    
                    // Store parent-child relationships
                    if (parentStation != null && !parentStation.isEmpty()) {
                        childToParentMap.put(stopId, parentStation);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load stops: " + e.getMessage());
        }
    }

    void loadTrips() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("gtfs/trips.txt").getInputStream()))) {
            reader.readLine(); // header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] p = line.split(",", -1);
                if (p.length >= 5) {
                    tripsMap.put(p[2], new Trip(p[2], p[1]));
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load trips: " + e.getMessage());
        }
    }

    
    /**
     * Gets the current date and time in Stockholm time zone
     * @return The current LocalDateTime
     */
    LocalDateTime getCurrentDateTime() {
        return timeProvider.now(STOCKHOLM_ZONE);
    }
    
    /**
     * Parses a GTFS time string (which can exceed 24 hours) into a LocalDateTime
     * using Stockholm time zone
     * @param gtfsTimeStr Time string in format "HH:MM:SS"
     * @return LocalDateTime object representing the time in Stockholm time zone
     */
    LocalDateTime parseGtfsTime(String gtfsTimeStr) {
        String[] parts = gtfsTimeStr.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid GTFS time format: " + gtfsTimeStr);
        }
        
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        int seconds = Integer.parseInt(parts[2]);
        
        // Handle times that exceed 24 hours
        LocalDate date = getCurrentDateTime().toLocalDate();
        if (hours >= 24) {
            hours = hours % 24;
            date = date.plusDays(1);
        }
        
        // Create LocalDateTime in Stockholm time zone
        return LocalDateTime.of(date, LocalTime.of(hours, minutes, seconds));
    }
    
    /**
     * Checks if the given time is within the next TIME_WINDOW_MINUTES from current time
     * @param time The time to check
     * @return true if the time is outside the window, false otherwise
     */
    boolean isOutsideTimeWindow(LocalDateTime time) {
        // Get the current time
        LocalDateTime now = getCurrentDateTime();
        
        // Time must be after or equal to the current time
        if (time.isBefore(now)) {
            return true;
        }
        
        // Time must be before current time + TIME_WINDOW_MINUTES
        LocalDateTime windowEnd = now.plusMinutes(TIME_WINDOW_MINUTES);
        return !time.isBefore(windowEnd) && !time.isEqual(windowEnd);
    }
    
    void loadStopTimes() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("gtfs/stop_times_extracted.txt").getInputStream()))) {
            reader.readLine(); // header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] p = line.split(",", -1);
                if (p.length >= 6) {
                    String tripId = p[0];
                    String arrivalTime = p[1];
                    String departureTime = p[2];
                    String stopId = p[3];
                    String stopHeadsign = p[5];

                    // Check if this stop is directly a target stop
                    boolean isTargetStop = TARGET_STOP_IDS.contains(stopId);
                    
                    // Check if this stop is a child of a target stop
                    String parentStopId = childToParentMap.get(stopId);
                    boolean isChildOfTargetStop = parentStopId != null && TARGET_STOP_IDS.contains(parentStopId);
                    
                    // Store stop times for both target stops and their children
                    if (isTargetStop || isChildOfTargetStop) {
                        StopTime stopTime = new StopTime(tripId, stopId, arrivalTime, departureTime, stopHeadsign);
                        
                        // For child stops, associate the stop time with the parent stop
                        String mapKey = isChildOfTargetStop ? parentStopId : stopId;
                        
                        // Add to the map, creating a new list if needed
                        stopTimesMap.computeIfAbsent(mapKey, k -> new ArrayList<>()).add(stopTime);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load stop times: " + e.getMessage());
        }
    }
    
    /**
     * Loads calendar data from calendar.txt and calendar_dates.txt to determine
     * which services are active on the current date.
     */
    void loadCalendarData() {
        // Format current date as YYYYMMDD for GTFS comparison
        String currentDateStr = getCurrentDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // Load calendar.txt to check service date ranges
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("gtfs/calendar.txt").getInputStream()))) {
            reader.readLine(); // header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] p = line.split(",", -1);
                if (p.length >= 10) {
                    String serviceId = p[0];
                    String startDate = p[8];
                    String endDate = p[9];
                    
                    // Check if current date is within service date range
                    if (currentDateStr.compareTo(startDate) >= 0 && currentDateStr.compareTo(endDate) <= 0) {
                        // In this case, all day-of-week fields are 0, so we rely on calendar_dates.txt
                        // for exceptions. We'll add this service to a temporary set for now.
                        activeServiceIds.add(serviceId);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load calendar data: " + e.getMessage());
        }
        
        // Load calendar_dates.txt to check for exceptions
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("gtfs/calendar_dates.txt").getInputStream()))) {
            reader.readLine(); // header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] p = line.split(",", -1);
                if (p.length >= 3) {
                    String serviceId = p[0];
                    String date = p[1];
                    String exceptionType = p[2];
                    
                    // Check if this exception applies to the current date
                    if (currentDateStr.equals(date)) {
                        if ("1".equals(exceptionType)) {
                            // Exception type 1: Service added on this date
                            activeServiceIds.add(serviceId);
                        } else if ("2".equals(exceptionType)) {
                            // Exception type 2: Service removed on this date
                            activeServiceIds.remove(serviceId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load calendar dates data: " + e.getMessage());
        }
        
        System.out.println("Loaded " + activeServiceIds.size() + " active services for date " + currentDateStr);
    }
}
