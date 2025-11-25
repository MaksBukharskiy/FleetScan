package com.fleetScan.taxiService.service;

import com.fleetScan.taxiService.repository.Admin.FleetRepository;
import com.fleetScan.taxiService.repository.Autopark.DriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class BotService {
    private final FleetRepository fleetRepository;
    private final DriverRepository driverRepository;

    @Autowired
    public BotService(FleetRepository fleetRepository, DriverRepository driverRepository) {
        this.fleetRepository = fleetRepository;
        this.driverRepository = driverRepository;
    }

    public String handleCommand(Long chatId, String message) {
        if(message.equals("/start")) {

            if(fleetRepository.existsByAdminChatId(chatId)) {
                return "\"Вы уже зарегистрированы как администратор автопарка!\""
            }

            return "Привет! Как называется ваш автопарк?";
        }

        if (!message.startsWith("/")) {
            return createNewFleet(chatId, message);
        }
    }

}
