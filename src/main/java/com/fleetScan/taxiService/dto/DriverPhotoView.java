package com.fleetScan.taxiService.dto;

import com.fleetScan.taxiService.domain.autopark.vehicle.VehicleStatus;

import java.time.LocalDateTime;

public record DriverPhotoView(
        Long id,
        String photoType,
        String plateNumber,
        VehicleStatus status,
        double confidence,
        String condition,
        String decisionReason,
        LocalDateTime detectedAt,
        String note,
        String photoUrl
) {
}
