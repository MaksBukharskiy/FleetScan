package com.fleetScan.taxiService.service.analytics;

import com.fleetScan.taxiService.domain.autopark.driver.UserRole;
import com.fleetScan.taxiService.domain.autopark.vehicle.DetectedVehicle;
import com.fleetScan.taxiService.domain.autopark.vehicle.VehicleStatus;
import com.fleetScan.taxiService.dto.DetectionUpdateRequest;
import com.fleetScan.taxiService.dto.DetectionView;
import com.fleetScan.taxiService.repository.autopark.vehicle.DetectedVehicleRepository;
import com.fleetScan.taxiService.service.security.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final DetectedVehicleRepository detectedVehicleRepository;
    private final AccessService accessService;

    public List<DetectionView> queue(Long chatId) {
        accessService.requireUser(chatId, UserRole.ADMIN, UserRole.OPERATOR, UserRole.OBSERVER);

        Specification<DetectedVehicle> specification = (root, query, cb) ->
                root.get("status").in(List.of(VehicleStatus.BLOCKED, VehicleStatus.UNDER_REVIEW));

        return detectedVehicleRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "detectedAt")).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional
    public DetectionView update(Long chatId, Long detectionId, DetectionUpdateRequest request) {
        accessService.requireUser(chatId, UserRole.ADMIN, UserRole.OPERATOR);

        DetectedVehicle detectedVehicle = detectedVehicleRepository.findById(detectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Detection not found"));

        if (request.status() != null) {
            detectedVehicle.setStatus(request.status());
        }
        String comment = request.comment() == null ? "" : request.comment().trim();
        String decisionReason = request.decisionReason() == null ? "" : request.decisionReason().trim();
        if (!decisionReason.isBlank()) {
            detectedVehicle.setDecisionReason(decisionReason);
        } else if (!comment.isBlank()) {
            detectedVehicle.setDecisionReason(comment);
        }

        return toView(detectedVehicleRepository.save(detectedVehicle));
    }

    private DetectionView toView(DetectedVehicle row) {
        return new DetectionView(
                row.getId(),
                row.getPlateNumber(),
                row.getStatus(),
                row.getConfidence(),
                row.getVehicleCondition(),
                row.getDecisionReason(),
                row.getDetectedAt(),
                "/api/admin/detections/" + row.getId() + "/photo"
        );
    }
}
