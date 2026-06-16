package com.fleetScan.taxiService.dto;

import com.fleetScan.taxiService.domain.autopark.driver.UserRole;

public record DriverUpdateRequest(
        String name,
        UserRole role,
        String vehiclePlate,
        String vehicleModel,
        Boolean isActive
) {
}
