package com.fleetScan.taxiService.dto;

public record WebSessionView(
        boolean authenticated,
        Long expiresAtEpochMillis
) {
}
