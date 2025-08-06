package com.ocklund.gtfs.configuration;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Interface for providing the current date and time.
 * This abstraction makes it easier to test time-dependent code.
 */
public interface TimeProvider {
    /**
     * Gets the current date and time in the specified time zone.
     * @param zoneId The time zone
     * @return The current LocalDateTime
     */
    LocalDateTime now(ZoneId zoneId);
}