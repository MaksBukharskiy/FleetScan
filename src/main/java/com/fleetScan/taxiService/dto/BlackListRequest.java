package com.fleetScan.taxiService.dto;

import com.fleetScan.taxiService.domain.autopark.vehicle.BlackListCategory;

import java.time.LocalDateTime;

public record BlackListRequest(
        String plateNumber,
        String reason,
        BlackListCategory category,
        LocalDateTime expiresAt
) {
}
