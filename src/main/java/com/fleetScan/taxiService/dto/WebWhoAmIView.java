package com.fleetScan.taxiService.dto;

public record WebWhoAmIView(
        boolean authenticated,
        Long chatId,
        String role,
        Long fleetId,
        String fleetName,
        Long expiresAtEpochMillis
) {
}

