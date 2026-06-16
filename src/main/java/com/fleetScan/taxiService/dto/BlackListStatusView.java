package com.fleetScan.taxiService.dto;

public record BlackListStatusView(
        String plateNumber,
        boolean active,
        String reason
) {
}
