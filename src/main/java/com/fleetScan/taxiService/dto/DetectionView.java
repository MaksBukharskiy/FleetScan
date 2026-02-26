package com.fleetScan.taxiService.dto;

import com.fleetScan.taxiService.domain.autopark.vehicle.VehicleStatus;

import java.time.LocalDateTime;

public record DetectionView(
        Long id,
        String plateNumber,
        VehicleStatus status,
        double confidence,
        String condition,
        String decisionReason,
        LocalDateTime detectedAt
) {
}
