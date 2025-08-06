package com.ocklund.gtfs.configuration;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Default implementation of TimeProvider that returns the actual current time.
 */
public class DefaultTimeProvider implements TimeProvider {
    
    @Override
    public LocalDateTime now(ZoneId zoneId) {
        return LocalDateTime.now(zoneId);
    }
}