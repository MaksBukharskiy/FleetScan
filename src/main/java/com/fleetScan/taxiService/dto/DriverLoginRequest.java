package com.fleetScan.taxiService.dto;

public record DriverLoginRequest(
        String accessToken,
        Long chatId
) {
}
