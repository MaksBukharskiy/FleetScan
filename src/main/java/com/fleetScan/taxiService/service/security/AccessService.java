package com.fleetScan.taxiService.service.security;

import com.fleetScan.taxiService.domain.autopark.driver.Driver;
import com.fleetScan.taxiService.domain.autopark.driver.UserRole;
import com.fleetScan.taxiService.repository.autopark.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class AccessService {

    private final DriverRepository driverRepository;

    public Driver requireUser(Long chatId, UserRole... allowedRoles) {
        
        
        if (Long.valueOf(0L).equals(chatId)) {
            Driver syntheticAdmin = new Driver();
            syntheticAdmin.setName("Web Admin");
            syntheticAdmin.setChatId(0L);
            syntheticAdmin.setIsActive(true);
            syntheticAdmin.setRole(UserRole.ADMIN);
            return syntheticAdmin;
        }

        Driver user = driverRepository.findByChatIdAndIsActiveTrue(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не зарегистрирован или не активен."));

        if (allowedRoles == null || allowedRoles.length == 0) {
            return user;
        }

        boolean allowed = Arrays.stream(allowedRoles).anyMatch(role -> role == user.getRole());
        if (!allowed) {
            throw new IllegalArgumentException("Недостаточно прав для этой операции.");
        }

        return user;
    }
}
