package com.fleetScan.taxiService.dto;

import com.fleetScan.taxiService.domain.autopark.driver.UserRole;

import java.time.LocalDateTime;

public record DriverView(
        Long id,
        String name,
        Long chatId,
        String inviteCode,
        boolean active,
        UserRole role,
        String vehiclePlate,
        String vehicleModel,
        String fleetName,
        long detectionCount,
        long photoCount,
        LocalDateTime createdAt
) {
}
