package com.ocklund.gtfs;

public record StopTime(String tripId, String stopId, String arrivalTime, String departureTime, String stopHeadsign) {
}