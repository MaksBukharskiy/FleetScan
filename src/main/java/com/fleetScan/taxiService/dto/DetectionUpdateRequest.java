package com.fleetScan.taxiService.dto;

import com.fleetScan.taxiService.domain.autopark.vehicle.VehicleStatus;

public record DetectionUpdateRequest(
        VehicleStatus status,
        String decisionReason,
        String comment
) {
}
