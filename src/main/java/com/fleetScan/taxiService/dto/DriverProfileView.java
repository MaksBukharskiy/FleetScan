package com.fleetScan.taxiService.dto;

import com.fleetScan.taxiService.domain.autopark.driver.UserRole;

public record DriverProfileView(
        Long id,
        String name,
        Long chatId,
        String fleetName,
        UserRole role,
        String vehiclePlate,
        String vehicleModel,
        long detectionCount,
        long photoCount
) {
}
