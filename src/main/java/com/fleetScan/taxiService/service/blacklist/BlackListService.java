package com.fleetScan.taxiService.service.blacklist;

import com.fleetScan.taxiService.domain.autopark.driver.Driver;
import com.fleetScan.taxiService.domain.autopark.driver.UserRole;
import com.fleetScan.taxiService.domain.autopark.vehicle.BlackList;
import com.fleetScan.taxiService.repository.autopark.vehicle.BlackListRepository;
import com.fleetScan.taxiService.service.security.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BlackListService {

    private final BlackListRepository blackListRepository;
    private final AccessService accessService;

    @Transactional
    public String addPlate(Long chatId, String plateNumber, String reason) {
        Driver actor = accessService.requireUser(chatId, UserRole.ADMIN, UserRole.OPERATOR);
        String normalized = normalizePlate(plateNumber);

        BlackList entity = blackListRepository.findByPlateNumber(normalized).orElseGet(BlackList::new);
        entity.setPlateNumber(normalized);
        entity.setReason(reason == null || reason.isBlank() ? "manual_block" : reason.trim());
        entity.setCreatedByChatId(actor.getChatId());
        entity.setIsActive(true);

        blackListRepository.save(entity);
        return "✅ Номер " + normalized + " добавлен в black list.";
    }

    @Transactional
    public String removePlate(Long chatId, String plateNumber) {
        accessService.requireUser(chatId, UserRole.ADMIN, UserRole.OPERATOR);
        String normalized = normalizePlate(plateNumber);

        Optional<BlackList> existing = blackListRepository.findByPlateNumberAndIsActiveTrue(normalized);
        if (existing.isEmpty()) {
            return "ℹ️ Номер " + normalized + " не найден в активном black list.";
        }

        BlackList entity = existing.get();
        entity.setIsActive(false);
        blackListRepository.save(entity);
        return "✅ Номер " + normalized + " удален из black list.";
    }

    public Optional<BlackList> findActiveByPlate(String plateNumber) {
        return blackListRepository.findByPlateNumberAndIsActiveTrue(normalizePlate(plateNumber));
    }

    public String statusByPlate(Long chatId, String plateNumber) {
        accessService.requireUser(chatId, UserRole.ADMIN, UserRole.OPERATOR, UserRole.OBSERVER);
        String normalized = normalizePlate(plateNumber);
        return blackListRepository.findByPlateNumberAndIsActiveTrue(normalized)
                .map(it -> "⛔ Номер " + normalized + " в black list. Причина: " + it.getReason())
                .orElse("✅ Номер " + normalized + " отсутствует в black list.");
    }

    public String normalizePlate(String plateNumber) {
        if (plateNumber == null) {
            return "";
        }
        return plateNumber
                .replaceAll("[^A-Za-zА-Яа-я0-9]", "")
                .toUpperCase();
    }
}
