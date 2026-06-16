package com.fleetScan.taxiService.dto;

import com.fleetScan.taxiService.domain.autopark.driver.UserRole;

public record DriverCreateRequest(
        String name,
        Long chatId,
        UserRole role,
        String vehiclePlate,
        String vehicleModel
) {
}
