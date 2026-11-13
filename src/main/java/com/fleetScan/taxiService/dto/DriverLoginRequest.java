package com.fleetScan.taxiService.dto;

/** code: one-time login code from the Telegram bot command /web. */
public record DriverLoginRequest(
        String code
) {
}
