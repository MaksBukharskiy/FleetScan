package com.fleetScan.taxiService.dto;

import com.fleetScan.taxiService.domain.autopark.vehicle.BlackListCategory;

import java.time.LocalDateTime;

public record BlackListView(
        Long id,
        String plateNumber,
        Long createdByChatId,
        boolean active,
        String reason,
        BlackListCategory category,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
