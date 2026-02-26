package com.fleetScan.taxiService.config;

import com.fleetScan.taxiService.integration.telegram.FleetScanBot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "telegram.bot.enabled", havingValue = "true", matchIfMissing = true)
public class BotConfig {

    private final FleetScanBot fleetScanBot;

    @PostConstruct
    public void init() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(fleetScanBot);
            log.info("✅ БОТ УСПЕШНО ЗАРЕГИСТРИРОВАН ВРУЧНУЮ!");
        } catch (TelegramApiException e) {
            log.error("❌ ОШИБКА РЕГИСТРАЦИИ БОТА", e);
        }
    }
}