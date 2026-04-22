package com.fleetScan.taxiService.service.analytics;

import com.fleetScan.taxiService.domain.autopark.driver.Driver;
import com.fleetScan.taxiService.domain.autopark.driver.UserRole;
import com.fleetScan.taxiService.domain.autopark.vehicle.DetectedVehicle;
import com.fleetScan.taxiService.domain.autopark.vehicle.VehicleStatus;
import com.fleetScan.taxiService.dto.AnalyticsSummary;
import com.fleetScan.taxiService.dto.DetectionView;
import com.fleetScan.taxiService.repository.autopark.DriverRepository;
import com.fleetScan.taxiService.repository.autopark.vehicle.DetectedVehicleRepository;
import com.fleetScan.taxiService.service.security.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final DetectedVehicleRepository detectedVehicleRepository;
    private final DriverRepository driverRepository;
    private final AccessService accessService;

    public AnalyticsSummary summary(Long chatId, LocalDateTime from, LocalDateTime to) {
        accessService.requireUser(chatId, UserRole.ADMIN, UserRole.OPERATOR, UserRole.OBSERVER);
        long total = detectedVehicleRepository.countByDetectedAtBetween(from, to);
        long active = detectedVehicleRepository.countByStatusAndDetectedAtBetween(VehicleStatus.ACTIVE, from, to);
        long blocked = detectedVehicleRepository.countByStatusAndDetectedAtBetween(VehicleStatus.BLOCKED, from, to);
        long review = detectedVehicleRepository.countByStatusAndDetectedAtBetween(VehicleStatus.UNDER_REVIEW, from, to);
        return new AnalyticsSummary(total, active, blocked, review);
    }

    public List<DetectionView> detections(
            Long chatId,
            String plate,
            VehicleStatus status,
            String decisionReason,
            Double confidenceFrom,
            Double confidenceTo,
            Long driverChatId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        accessService.requireUser(chatId, UserRole.ADMIN, UserRole.OPERATOR, UserRole.OBSERVER);

        Long normalizedDriverChatId = resolveDriverChatId(driverChatId);

        Specification<DetectedVehicle> specification = Specification
                .where(dateRange(from, to))
                .and(plateContains(plate))
                .and(statusEquals(status))
                .and(reasonContains(decisionReason))
                .and(confidenceGte(confidenceFrom))
                .and(confidenceLte(confidenceTo))
                .and(sourceChatIdEquals(normalizedDriverChatId));

        return detectedVehicleRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "detectedAt")).stream()
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

    public List<DetectionView> incidentRows(
            Long chatId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        accessService.requireUser(chatId, UserRole.ADMIN, UserRole.OPERATOR, UserRole.OBSERVER);

        Specification<DetectedVehicle> specification = Specification
                .where(dateRange(from, to))
                .and((root, query, cb) -> root.get("status").in(List.of(VehicleStatus.BLOCKED, VehicleStatus.UNDER_REVIEW)));

        return detectedVehicleRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "detectedAt")).stream()
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

    public List<DriverComplianceRow> driverCompliance(Long chatId) {
        accessService.requireUser(chatId, UserRole.ADMIN, UserRole.OPERATOR, UserRole.OBSERVER);

        return driverRepository.findAll().stream()
                .map(driver -> {
                    Long sourceChatId = driver.getChatId();
                    List<DetectedVehicle> rows = sourceChatId == null
                            ? List.of()
                            : detectedVehicleRepository.findAllBySourceChatIdOrderByDetectedAtDesc(sourceChatId);

                    long blocked = rows.stream().filter(item -> item.getStatus() == VehicleStatus.BLOCKED).count();
                    long review = rows.stream().filter(item -> item.getStatus() == VehicleStatus.UNDER_REVIEW).count();

                    return new DriverComplianceRow(
                            driver.getId(),
                            driver.getName(),
                            driver.getChatId(),
                            driver.getFleet() == null ? null : driver.getFleet().getName(),
                            driver.getRole() == null ? null : driver.getRole().name(),
                            rows.size(),
                            blocked,
                            review,
                            blocked == 0 && review == 0 ? "COMPLIANT" : "REQUIRES_ATTENTION"
                    );
                })
                .toList();
    }

    private Long resolveDriverChatId(Long driverChatId) {
        if (driverChatId == null) {
            return null;
        }
        Driver byDriverId = driverRepository.findById(driverChatId).orElse(null);
        if (byDriverId != null && byDriverId.getChatId() != null) {
            return byDriverId.getChatId();
        }
        return driverChatId;
    }

    private Specification<DetectedVehicle> dateRange(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> cb.between(root.get("detectedAt"), from, to);
    }

    private Specification<DetectedVehicle> plateContains(String plate) {
        if (plate == null || plate.isBlank()) {
            return null;
        }
        String pattern = "%" + plate.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("plateNumber")), pattern);
    }

    private Specification<DetectedVehicle> statusEquals(VehicleStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private Specification<DetectedVehicle> reasonContains(String decisionReason) {
        if (decisionReason == null || decisionReason.isBlank()) {
            return null;
        }
        String pattern = "%" + decisionReason.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("decisionReason")), pattern);
    }

    private Specification<DetectedVehicle> confidenceGte(Double confidenceFrom) {
        if (confidenceFrom == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("confidence"), confidenceFrom);
    }

    private Specification<DetectedVehicle> confidenceLte(Double confidenceTo) {
        if (confidenceTo == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("confidence"), confidenceTo);
    }

    private Specification<DetectedVehicle> sourceChatIdEquals(Long sourceChatId) {
        if (sourceChatId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("sourceChatId"), sourceChatId);
    }

    public record DriverComplianceRow(
            Long driverId,
            String driverName,
            Long chatId,
            String fleetName,
            String role,
            long totalDetections,
            long blockedCount,
            long underReviewCount,
            String complianceStatus
    ) {
    }
}
