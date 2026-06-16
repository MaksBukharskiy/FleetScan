package com.fleetScan.taxiService.service.driver;

import com.fleetScan.taxiService.domain.autopark.car.CarPhoto;
import com.fleetScan.taxiService.domain.autopark.car.PhotoType;
import com.fleetScan.taxiService.domain.autopark.driver.Driver;
import com.fleetScan.taxiService.domain.autopark.vehicle.DetectedVehicle;
import com.fleetScan.taxiService.dto.DetectionView;
import com.fleetScan.taxiService.dto.DriverPhotoUploadRequest;
import com.fleetScan.taxiService.dto.DriverPhotoView;
import com.fleetScan.taxiService.dto.DriverProfileView;
import com.fleetScan.taxiService.dto.PhotoContent;
import com.fleetScan.taxiService.repository.autopark.DriverRepository;
import com.fleetScan.taxiService.repository.autopark.car.CarPhotoRepository;
import com.fleetScan.taxiService.repository.autopark.vehicle.DetectedVehicleRepository;
import com.fleetScan.taxiService.service.security.DriverWebAuthService;
import com.fleetScan.taxiService.service.storage.PhotoStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DriverPortalService {

    private final DriverWebAuthService driverWebAuthService;
    private final CarPhotoRepository carPhotoRepository;
    private final DetectedVehicleRepository detectedVehicleRepository;
    private final PhotoStorageService photoStorageService;

    public DriverProfileView profile(String authorizationHeader) {
        Driver driver = driverWebAuthService.requireDriver(authorizationHeader);
        return toProfile(driver);
    }

    public List<DriverPhotoView> photos(String authorizationHeader) {
        Driver driver = driverWebAuthService.requireDriver(authorizationHeader);
        return carPhotoRepository.findAllByDriverIdOrderByCreatedAtDesc(driver.getId()).stream()
                .map(photo -> toPhotoView(driver, photo))
                .toList();
    }

    public List<DetectionView> detections(String authorizationHeader) {
        Driver driver = driverWebAuthService.requireDriver(authorizationHeader);
        if (driver.getChatId() == null) {
            return List.of();
        }
        return detectedVehicleRepository.findAllBySourceChatIdOrderByDetectedAtDesc(driver.getChatId()).stream()
                .map(this::toDetectionView)
                .toList();
    }

    @Transactional
    public DriverPhotoView upload(String authorizationHeader, MultipartFile file, DriverPhotoUploadRequest request) {
        Driver driver = driverWebAuthService.requireDriver(authorizationHeader);
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo file is required");
        }

        String storedPath = photoStorageService.store(file);
        CarPhoto photo = new CarPhoto();
        photo.setDriver(driver);
        photo.setTelegramFileId("web-" + UUID.randomUUID());
        photo.setFilePath(storedPath);
        photo.setPhotoType(request.photoType() == null ? PhotoType.OTHER : request.photoType());
        photo.setStatus("UPLOADED_WEB");
        photo.setNote(request.note());

        CarPhoto saved = carPhotoRepository.save(photo);
        return toPhotoView(driver, saved);
    }

    public PhotoContent photoContent(String authorizationHeader, Long photoId) {
        Driver driver = driverWebAuthService.requireDriver(authorizationHeader);
        CarPhoto photo = carPhotoRepository.findByIdAndDriverId(photoId, driver.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found"));
        if (photo.getFilePath() == null || photo.getFilePath().isBlank() || !photoStorageService.exists(photo.getFilePath())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo file not available");
        }
        return new PhotoContent(photoStorageService.read(photo.getFilePath()), photoStorageService.contentType(photo.getFilePath()));
    }

    public PhotoContent detectionPhotoContent(String authorizationHeader, Long detectionId) {
        Driver driver = driverWebAuthService.requireDriver(authorizationHeader);
        DetectedVehicle vehicle = detectedVehicleRepository.findById(detectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Detection not found"));
        if (vehicle.getSourceChatId() == null || !vehicle.getSourceChatId().equals(driver.getChatId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Detection belongs to another driver");
        }
        if (vehicle.getImagePath() == null || vehicle.getImagePath().isBlank() || !photoStorageService.exists(vehicle.getImagePath())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo file not available");
        }
        return new PhotoContent(photoStorageService.read(vehicle.getImagePath()), photoStorageService.contentType(vehicle.getImagePath()));
    }

    private DriverProfileView toProfile(Driver driver) {
        long detectionCount = driver.getChatId() == null ? 0L : detectedVehicleRepository.countBySourceChatId(driver.getChatId());
        long photoCount = carPhotoRepository.countByDriverId(driver.getId());
        return new DriverProfileView(
                driver.getId(),
                driver.getName(),
                driver.getChatId(),
                driver.getFleet() == null ? null : driver.getFleet().getName(),
                driver.getRole(),
                driver.getVehiclePlate(),
                driver.getVehicleModel(),
                detectionCount,
                photoCount
        );
    }

    private DriverPhotoView toPhotoView(Driver driver, CarPhoto photo) {
        return new DriverPhotoView(
                photo.getId(),
                photo.getPhotoType() == null ? null : photo.getPhotoType().name(),
                driver.getVehiclePlate(),
                null,
                0.0,
                "photo_upload",
                photo.getStatus(),
                photo.getCreatedAt(),
                photo.getNote(),
                "/api/driver/photos/" + photo.getId() + "/content"
        );
    }

    private DetectionView toDetectionView(DetectedVehicle vehicle) {
        return new DetectionView(
                vehicle.getId(),
                vehicle.getPlateNumber(),
                vehicle.getStatus(),
                vehicle.getConfidence(),
                vehicle.getVehicleCondition(),
                vehicle.getDecisionReason(),
                vehicle.getDetectedAt(),
                "/api/driver/detections/" + vehicle.getId() + "/photo"
        );
    }
}
