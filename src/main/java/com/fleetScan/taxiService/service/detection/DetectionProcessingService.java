package com.fleetScan.taxiService.service.detection;

import com.fleetScan.taxiService.domain.autopark.vehicle.BlackList;
import com.fleetScan.taxiService.domain.autopark.vehicle.DetectedVehicle;
import com.fleetScan.taxiService.domain.autopark.vehicle.VehicleStatus;
import com.fleetScan.taxiService.repository.autopark.vehicle.DetectedVehicleRepository;
import com.fleetScan.taxiService.dto.AiAnalysisResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DetectionProcessingService {

    private final DetectedVehicleRepository detectedVehicleRepository;
    private final BlackListService blackListService;

    @Value("${fleetscan.ai.min-confidence:0.80}")
    private double minConfidence;

    @Transactional
    public DetectedVehicle process(Long sourceChatId, String imagePath, AiAnalysisResult ai) {
        String normalizedPlate = blackListService.normalizePlate(ai.plateNumber());
        Optional<BlackList> blackListHit = blackListService.findActiveByPlate(normalizedPlate);

        VehicleStatus status;
        String reason;
        if (blackListHit.isPresent()) {
            status = VehicleStatus.BLOCKED;
            reason = "blacklist_match";
        } else if (ai.plateConfidence() < minConfidence || "defect".equalsIgnoreCase(ai.condition())) {
            status = VehicleStatus.UNDER_REVIEW;
            reason = ai.plateConfidence() < minConfidence ? "low_confidence" : "damage_detected";
        } else {
            status = VehicleStatus.ACTIVE;
            reason = "auto_passed";
        }

        DetectedVehicle detectedVehicle = new DetectedVehicle();
        detectedVehicle.setPlateNumber(normalizedPlate.isBlank() ? "UNKNOWN" : normalizedPlate);
        detectedVehicle.setConfidence(ai.plateConfidence());
        detectedVehicle.setVehicleCondition(ai.condition() == null ? "unknown" : ai.condition());
        detectedVehicle.setConditionConfidence(ai.conditionConfidence());
        detectedVehicle.setStatus(status);
        detectedVehicle.setDecisionReason(reason);
        detectedVehicle.setSourceChatId(sourceChatId);
        detectedVehicle.setImagePath(imagePath);
        detectedVehicle.setBlackList(blackListHit.orElse(null));

        return detectedVehicleRepository.save(detectedVehicle);
    }
}
