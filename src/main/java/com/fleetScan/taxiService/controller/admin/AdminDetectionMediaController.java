package com.fleetScan.taxiService.controller.admin;

import com.fleetScan.taxiService.domain.autopark.vehicle.DetectedVehicle;
import com.fleetScan.taxiService.repository.autopark.vehicle.DetectedVehicleRepository;
import com.fleetScan.taxiService.service.security.WebAuthService;
import com.fleetScan.taxiService.service.storage.PhotoStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/detections")
public class AdminDetectionMediaController {

    private final DetectedVehicleRepository detectedVehicleRepository;
    private final WebAuthService webAuthService;
    private final PhotoStorageService photoStorageService;

    @GetMapping("/{detectionId}/photo")
    public ResponseEntity<byte[]> photoContent(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable Long detectionId
    ) {
        webAuthService.requireChatId(authorization);
        DetectedVehicle detectedVehicle = detectedVehicleRepository.findById(detectionId)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Detection not found"));
        if (detectedVehicle.getImagePath() == null || detectedVehicle.getImagePath().isBlank() || !photoStorageService.exists(detectedVehicle.getImagePath())) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Photo file not available");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photoStorageService.contentType(detectedVehicle.getImagePath())))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(photoStorageService.read(detectedVehicle.getImagePath()));
    }
}
