package com.fleetScan.taxiService.service.detection;

import com.fleetScan.taxiService.domain.autopark.driver.Driver;
import com.fleetScan.taxiService.domain.autopark.vehicle.BlackList;
import com.fleetScan.taxiService.domain.autopark.vehicle.DetectedVehicle;
import com.fleetScan.taxiService.domain.autopark.vehicle.VehicleStatus;
import com.fleetScan.taxiService.repository.autopark.DriverRepository;
import com.fleetScan.taxiService.repository.autopark.vehicle.DetectedVehicleRepository;
import com.fleetScan.taxiService.dto.AiAnalysisResult;
import com.fleetScan.taxiService.service.blacklist.BlackListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DetectionProcessingService {

    private final DetectedVehicleRepository detectedVehicleRepository;
    private final BlackListService blackListService;
    private final DriverRepository driverRepository;

    @Value("${fleetscan.ai.min-confidence:0.80}")
    private double minConfidence;

    @Transactional
    public DetectedVehicle process(Long sourceChatId, String imagePath, AiAnalysisResult ai) {
        String normalizedPlate = blackListService.normalizePlate(ai.plateNumber());
        Optional<BlackList> blackListHit = blackListService.findActiveByPlate(normalizedPlate);

        Optional<Driver> driverOpt = driverRepository.findByChatIdAndIsActiveTrue(sourceChatId);
        String driverPlate = driverOpt
                .flatMap(d -> Optional.ofNullable(d.getVehiclePlate()))
                .map(blackListService::normalizePlate)
                .orElse(null);

        boolean plateMismatch = driverPlate != null && !driverPlate.isBlank() 
                                && !normalizedPlate.isBlank()
                                && !driverPlate.equals(normalizedPlate);

        VehicleStatus status;
        String reason;
        if (blackListHit.isPresent()) {
            status = VehicleStatus.BLOCKED;
            reason = "blacklist_match";
        } else if (plateMismatch) {
            status = VehicleStatus.UNDER_REVIEW;
            reason = "plate_mismatch";
            log.warn("⚠️ Несоответствие номера: распознан={}, зарегистрирован={}, chatId={}", 
                    normalizedPlate, driverPlate, sourceChatId);
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
