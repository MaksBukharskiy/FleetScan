package com.fleetScan.taxiService.service.Bot;

import com.fleetScan.taxiService.entity.Admin.Fleet;
import com.fleetScan.taxiService.repository.Admin.FleetRepository;
import com.fleetScan.taxiService.repository.Autopark.DriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class BotService {
    private final FleetRepository fleetRepository;
    private final DriverRepository driverRepository;

    private Map <Long, String> userStates = new HashMap<>();

    private static final String AWAITING_FLEET_NAME = "AWAITING_FLEET_NAME";
    private static final String AWAITING_DRIVER_NAME = "AWAITING_DRIVER_NAME";


    @Autowired
    public BotService(FleetRepository fleetRepository, DriverRepository driverRepository) {
        this.fleetRepository = fleetRepository;
        this.driverRepository = driverRepository;
    }

    public String handleMessage(Long chatId, String message) {
        if(message == null || message.isEmpty() || message.isBlank()) return "❌ Пустое сообщение.";

        log.info("Получено от {}: {}", chatId, message);

        String state = userStates.get(chatId);

        if (AWAITING_FLEET_NAME.equals(state)) {
            return createNewFleet(chatId, message);
        }
        if (AWAITING_DRIVER_NAME.equals(state)) {
            return addNewDriver(chatId, message);
        }


        if(message.equals("/start")){
            return handleStartCommand(chatId);
        }
        if ("/add_driver".equals(message)) {
            return startToAddDriver(chatId);
        }

        return "Неизвестная команда. Используйте /start";
    }

    private String handleStartCommand(Long chatId) {
        Optional<Fleet> existingCheckingFleet = fleetRepository.findByAdminChatId(chatId);

        if (existingCheckingFleet.isPresent()) {
            return String.format("✅ Вы уже админ '%s'. ID: %s",
                    existingCheckingFleet.get().getName(), existingCheckingFleet.get().getPublicId());
        }

        userStates.put(chatId, AWAITING_FLEET_NAME);

        return "\uD83D\uDC4B Введите название автопарка: ";
    }

    private String createNewFleet(Long chatId, String name) {
        name = name.trim();

        if (name == null || name.isEmpty() || name.isBlank()) {
            return "Ошибка в имени,\n попробуйте другое ❌";
        }

        if(name.length()<3){
            return "❌ Слишком короткое имя,\nпопробуйте другое";
        }

        if (fleetRepository.existsByName(name)) {
            userStates.remove(chatId);
            return "❌ Уже есть такой автопарк.";
        }

        Fleet fleet = new Fleet();
        fleet.setAdminChatId(chatId);
        fleet.setName(name);
        fleet.setPublicId("TAXI" + (int)(Math.random() * 900 + 100));
        fleetRepository.save(fleet);

        userStates.remove(chatId);
        log.info("Создан автопарк: {} (ID: {})", name, fleet.getPublicId());

        return String.format("✅ Готово! Ваш ID: %s\nТеперь /add_driver", fleet.getPublicId());
    }

    public String startToAddDriver(Long chatId) {

        if(!fleetRepository.existsByAdminChatId(chatId)) {
            return "❌ Сначала создайте автопарк через /start";
        }

        userStates.put(chatId, AWAITING_DRIVER_NAME);

        return "✏️ Введите ФИО водителя:";
    }

    private String addNewDriver(Long chatId, String driverName) {



    }

}
