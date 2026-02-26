package com.fleetScan.taxiService.service.analytics;

import com.fleetScan.taxiService.domain.autopark.driver.UserRole;
import com.fleetScan.taxiService.domain.autopark.vehicle.DetectedVehicle;
import com.fleetScan.taxiService.domain.autopark.vehicle.VehicleStatus;
import com.fleetScan.taxiService.repository.autopark.vehicle.DetectedVehicleRepository;
import com.fleetScan.taxiService.dto.AnalyticsSummary;
import com.fleetScan.taxiService.dto.DetectionView;
import com.fleetScan.taxiService.service.security.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final DetectedVehicleRepository detectedVehicleRepository;
    private final AccessService accessService;

    public AnalyticsSummary summary(Long chatId, LocalDateTime from, LocalDateTime to) {
        accessService.requireUser(chatId, UserRole.ADMIN, UserRole.OBSERVER);
        long total = detectedVehicleRepository.countByDetectedAtBetween(from, to);
        long active = detectedVehicleRepository.countByStatusAndDetectedAtBetween(VehicleStatus.ACTIVE, from, to);
        long blocked = detectedVehicleRepository.countByStatusAndDetectedAtBetween(VehicleStatus.BLOCKED, from, to);
        long review = detectedVehicleRepository.countByStatusAndDetectedAtBetween(VehicleStatus.UNDER_REVIEW, from, to);
        return new AnalyticsSummary(total, active, blocked, review);
    }

    public List<DetectionView> detections(Long chatId, String plate, VehicleStatus status, LocalDateTime from, LocalDateTime to) {
        accessService.requireUser(chatId, UserRole.ADMIN, UserRole.OBSERVER);

        List<DetectedVehicle> rows;
        if (plate != null && !plate.isBlank()) {
            rows = detectedVehicleRepository.findAllByPlateNumberContainingIgnoreCaseAndDetectedAtBetweenOrderByDetectedAtDesc(
                    plate, from, to
            );
        } else if (status != null) {
            rows = detectedVehicleRepository.findAllByStatusAndDetectedAtBetweenOrderByDetectedAtDesc(status, from, to);
        } else {
            rows = detectedVehicleRepository.findAllByDetectedAtBetweenOrderByDetectedAtDesc(from, to);
        }

        return rows.stream()
                .map(row -> new DetectionView(
                        row.getId(),
                        row.getPlateNumber(),
                        row.getStatus(),
                        row.getConfidence(),
                        row.getVehicleCondition(),
                        row.getDecisionReason(),
                        row.getDetectedAt()
                ))
                .toList();
    }
}
