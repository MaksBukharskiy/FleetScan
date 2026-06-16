package com.fleetScan.taxiService.dto;

public record DriverSessionView(
        boolean authenticated,
        Long driverId,
        String driverName,
        Long expiresAtEpochMillis
) {
}
