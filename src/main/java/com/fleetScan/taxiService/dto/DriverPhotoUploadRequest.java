package com.fleetScan.taxiService.dto;

import com.fleetScan.taxiService.domain.autopark.car.PhotoType;

public record DriverPhotoUploadRequest(
        PhotoType photoType,
        String note
) {
}
