package com.fleetScan.taxiService.service.notification;

import com.fleetScan.taxiService.domain.autopark.driver.Driver;
import com.fleetScan.taxiService.domain.autopark.driver.UserRole;
import com.fleetScan.taxiService.domain.autopark.vehicle.DetectedVehicle;
import com.fleetScan.taxiService.repository.autopark.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationsService {

    private final DriverRepository driverRepository;

    public List<Long> resolveSecurityRecipients(Long sourceChatId) {
        Driver source = driverRepository.findByChatIdAndIsActiveTrue(sourceChatId).orElse(null);
        if (source == null || source.getFleet() == null) {
            return List.of();
        }
        return driverRepository
                .findAllByFleetIdAndRoleInAndIsActiveTrue(
                        source.getFleet().getId(),
                        List.of(UserRole.ADMIN)
                )
                .stream()
                .map(Driver::getChatId)
                .filter(chatId -> chatId != null)
                .distinct()
                .collect(Collectors.toList());
    }

    public String buildSecurityMessage(DetectedVehicle vehicle) {
        StringBuilder sb = new StringBuilder();
        sb.append("🚨 Security event\n");
        sb.append("Plate: ").append(vehicle.getPlateNumber()).append("\n");
        sb.append("Status: ").append(vehicle.getStatus()).append("\n");
        sb.append("Reason: ").append(vehicle.getDecisionReason());
        
        if ("plate_mismatch".equals(vehicle.getDecisionReason())) {
            sb.append("\n⚠️ Номер не совпадает с зарегистрированным!");
        }
        
        sb.append("\nDetected at: ").append(vehicle.getDetectedAt());
        return sb.toString();
    }
}
