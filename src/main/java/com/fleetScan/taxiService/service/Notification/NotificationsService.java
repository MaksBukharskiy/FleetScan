package com.fleetScan.taxiService.service.notification;

import com.fleetScan.taxiService.domain.autopark.driver.Driver;
import com.fleetScan.taxiService.domain.autopark.driver.UserRole;
import com.fleetScan.taxiService.domain.autopark.vehicle.DetectedVehicle;
import com.fleetScan.taxiService.repository.autopark.DriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
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
                        List.of(UserRole.ADMIN, UserRole.OPERATOR)
                )
                .stream()
                .map(Driver::getChatId)
                .filter(chatId -> chatId != null)
                .distinct()
                .collect(Collectors.toList());
    }

    public String buildSecurityMessage(DetectedVehicle vehicle) {
        return "🚨 Security event\n" +
                "Plate: " + vehicle.getPlateNumber() + "\n" +
                "Status: " + vehicle.getStatus() + "\n" +
                "Reason: " + vehicle.getDecisionReason() + "\n" +
                "Detected at: " + vehicle.getDetectedAt();
    }

    @Scheduled(cron = "0 0 12 * * *")
    public void remindDriver() {
        log.info("⏰ Запуск ежедневной проверки напоминаний");
    }
}
