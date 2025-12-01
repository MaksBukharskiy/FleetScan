package com.fleetScan.taxiService.config;

import com.fleetScan.taxiService.service.BotCommunication.FleetScanBot;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class BotConfig {

    private final FleetScanBot fleetScanBot;

    @PostConstruct
    public void init() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(fleetScanBot);
            System.out.println("✅ БОТ УСПЕШНО ЗАРЕГИСТРИРОВАН ВРУЧНУЮ!");
        } catch (TelegramApiException e) {
            System.err.println("❌ ОШИБКА РЕГИСТРАЦИИ БОТА: " + e.getMessage());
            e.printStackTrace();
        }
    }
}