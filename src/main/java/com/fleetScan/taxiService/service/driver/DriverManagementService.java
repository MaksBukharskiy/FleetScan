package com.fleetScan.taxiService.service.driver;

import com.fleetScan.taxiService.domain.admin.Fleet;
import com.fleetScan.taxiService.domain.autopark.car.CarPhoto;
import com.fleetScan.taxiService.domain.autopark.driver.Driver;
import com.fleetScan.taxiService.domain.autopark.driver.UserRole;
import com.fleetScan.taxiService.domain.autopark.vehicle.VehicleStatus;
import com.fleetScan.taxiService.dto.DriverCreateRequest;
import com.fleetScan.taxiService.dto.DriverPhotoView;
import com.fleetScan.taxiService.dto.DriverUpdateRequest;
import com.fleetScan.taxiService.dto.DriverView;
import com.fleetScan.taxiService.dto.DetectionView;
import com.fleetScan.taxiService.dto.PhotoContent;
import com.fleetScan.taxiService.repository.admin.FleetRepository;
import com.fleetScan.taxiService.repository.autopark.DriverRepository;
import com.fleetScan.taxiService.repository.autopark.car.CarPhotoRepository;
import com.fleetScan.taxiService.repository.autopark.vehicle.DetectedVehicleRepository;
import com.fleetScan.taxiService.service.blacklist.BlackListService;
import com.fleetScan.taxiService.service.security.AccessService;
import com.fleetScan.taxiService.service.storage.PhotoStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DriverManagementService {

    private final AccessService accessService;
    private final FleetRepository fleetRepository;
    private final DriverRepository driverRepository;
    private final CarPhotoRepository carPhotoRepository;
    private final DetectedVehicleRepository detectedVehicleRepository;
    private final BlackListService blackListService;
    private final PhotoStorageService photoStorageService;

    public List<DriverView> list(Long chatId) {
        Driver admin = accessService.requireUser(chatId, UserRole.ADMIN);
        Fleet fleet = requireFleet(admin);
        return driverRepository.findAllByFleetId(fleet.getId()).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional
    public DriverView create(Long chatId, DriverCreateRequest request) {
        Driver admin = accessService.requireUser(chatId, UserRole.ADMIN);
        Fleet fleet = requireFleet(admin);

        String name = normalizeName(request.name());
        if (name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Driver name is required");
        }
        if (request.chatId() != null && driverRepository.findByChatId(request.chatId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chat id already exists");
        }

        Driver driver = new Driver();
        driver.setFleet(fleet);
        driver.setName(name);
        driver.setChatId(request.chatId());
        driver.setInviteCode("DRV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        driver.setIsActive(true);
        driver.setRole(request.role() == null ? UserRole.OPERATOR : request.role());
        driver.setVehiclePlate(normalizePlate(request.vehiclePlate()));
        driver.setVehicleModel(normalizeName(request.vehicleModel()));

        return toView(driverRepository.save(driver));
    }

    @Transactional
    public DriverView update(Long chatId, Long driverId, DriverUpdateRequest request) {
        Driver admin = accessService.requireUser(chatId, UserRole.ADMIN);
        Fleet fleet = requireFleet(admin);
        Driver driver = requireDriverInFleet(driverId, fleet.getId());

        if (request.name() != null && !request.name().isBlank()) {
            driver.setName(normalizeName(request.name()));
        }
        if (request.role() != null) {
            driver.setRole(request.role());
        }
        if (request.vehiclePlate() != null) {
            String plate = normalizePlate(request.vehiclePlate());
            driver.setVehiclePlate(plate.isBlank() ? null : plate);
        }
        if (request.vehicleModel() != null) {
            String model = normalizeName(request.vehicleModel());
            driver.setVehicleModel(model.isBlank() ? null : model);
        }
        if (request.isActive() != null) {
            driver.setIsActive(request.isActive());
        }

        return toView(driverRepository.save(driver));
    }

    @Transactional
    public void delete(Long chatId, Long driverId) {
        Driver admin = accessService.requireUser(chatId, UserRole.ADMIN);
        Fleet fleet = requireFleet(admin);
        Driver driver = requireDriverInFleet(driverId, fleet.getId());
        driver.setIsActive(false);
        driverRepository.save(driver);
    }

    public List<DriverPhotoView> photos(Long chatId, Long driverId) {
        Driver admin = accessService.requireUser(chatId, UserRole.ADMIN);
        Fleet fleet = requireFleet(admin);
        Driver driver = requireDriverInFleet(driverId, fleet.getId());
        return carPhotoRepository.findAllByDriverIdOrderByCreatedAtDesc(driver.getId()).stream()
                .map(photo -> toPhotoView(driver, photo))
                .toList();
    }

    public PhotoContent photoContent(Long chatId, Long driverId, Long photoId) {
        Driver admin = accessService.requireUser(chatId, UserRole.ADMIN);
        Fleet fleet = requireFleet(admin);
        Driver driver = requireDriverInFleet(driverId, fleet.getId());
        CarPhoto photo = carPhotoRepository.findByIdAndDriverId(photoId, driver.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found"));
        if (photo.getFilePath() == null || photo.getFilePath().isBlank() || !photoStorageService.exists(photo.getFilePath())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo file not available");
        }
        return new PhotoContent(photoStorageService.read(photo.getFilePath()), photoStorageService.contentType(photo.getFilePath()));
    }

    public List<DetectionView> detections(Long chatId, Long driverId) {
        Driver admin = accessService.requireUser(chatId, UserRole.ADMIN, UserRole.OPERATOR, UserRole.OBSERVER);
        Fleet fleet = requireFleet(admin);
        Driver driver = requireDriverInFleet(driverId, fleet.getId());
        if (driver.getChatId() == null) {
            return List.of();
        }

        return detectedVehicleRepository.findAllBySourceChatIdOrderByDetectedAtDesc(driver.getChatId()).stream()
                .map(row -> new DetectionView(
                        row.getId(),
                        row.getPlateNumber(),
                        row.getStatus(),
                        row.getConfidence(),
                        row.getVehicleCondition(),
                        row.getDecisionReason(),
                        row.getDetectedAt(),
                        "/api/admin/detections/" + row.getId() + "/photo"
                ))
                .toList();
    }

    private DriverView toView(Driver driver) {
        long detectionCount = driver.getChatId() == null ? 0L : detectedVehicleRepository.countBySourceChatId(driver.getChatId());
        long photoCount = carPhotoRepository.countByDriverId(driver.getId());
        return new DriverView(
                driver.getId(),
                driver.getName(),
                driver.getChatId(),
                driver.getInviteCode(),
                Boolean.TRUE.equals(driver.getIsActive()),
                driver.getRole(),
                driver.getVehiclePlate(),
                driver.getVehicleModel(),
                driver.getFleet() == null ? null : driver.getFleet().getName(),
                detectionCount,
                photoCount,
                driver.getCreatedAt()
        );
    }

    private DriverPhotoView toPhotoView(Driver driver, CarPhoto photo) {
        return new DriverPhotoView(
                photo.getId(),
                photo.getPhotoType() == null ? null : photo.getPhotoType().name(),
                driver.getVehiclePlate(),
                VehicleStatus.ACTIVE,
                0.0,
                "photo_upload",
                photo.getStatus(),
                photo.getCreatedAt(),
                photo.getNote(),
                "/api/admin/drivers/" + driver.getId() + "/photos/" + photo.getId() + "/content"
        );
    }

    private Fleet requireFleet(Driver admin) {
        if (admin.getFleet() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Fleet not found");
        }
        return fleetRepository.findById(admin.getFleet().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Fleet not found"));
    }

    private Driver requireDriverInFleet(Long driverId, Long fleetId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found"));
        if (driver.getFleet() == null || !fleetId.equals(driver.getFleet().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Driver belongs to another fleet");
        }
        return driver;
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizePlate(String value) {
        return blackListService.normalizePlate(value);
    }
}
