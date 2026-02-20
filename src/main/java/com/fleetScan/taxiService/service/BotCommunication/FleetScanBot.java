package com.fleetScan.taxiService.service.BotCommunication;

import com.fleetScan.taxiService.service.Bot.BotService;
import com.fleetScan.taxiService.service.FleetAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.nio.file.Files;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
@RequiredArgsConstructor
public class FleetScanBot extends TelegramLongPollingBot {

    private final BotService botService;

    @Autowired
    private FleetAiService fleetAiService;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.name}")
    private String botName;

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {

        try {

            if (update.hasMessage() && update.getMessage().hasText()) {
                handleText(update);
                return;
            }

            if (update.hasMessage() && update.getMessage().hasPhoto()) {
                handlePhoto(update);
                return;
            }

        } catch (Exception e) {
            log.error("❌ Ошибка обработки update", e);
        }
    }

    private void handleText(Update update) {
        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        String state = botService.getUserState(chatId);

        if ("AWAITING_FLEET_NAME".equals(state)) {
            sendMessage(chatId, botService.createNewFleet(chatId, text));
            return;
        }

        if ("AWAITING_DRIVER_NAME".equals(state)) {
            sendMessage(chatId, botService.addNewDriver(chatId, text));
            return;
        }

        if (text.startsWith("/start")) {
            String[] parts = text.split(" ", 2);

            if (parts.length > 1) {
                sendMessage(chatId, botService.handleInviteLink(chatId, parts[1]));
            } else {
                sendMessage(chatId, botService.handleStartCommand(chatId));
            }
            return;
        }

        if ("/add_driver".equals(text)) {
            sendMessage(chatId, botService.startToAddDriver(chatId));
            return;
        }

        sendMessage(chatId, "Неизвестная команда. Используйте /start");
    }

    private void handlePhoto(Update update) {

        Long chatId = update.getMessage().getChatId();
        Message message = update.getMessage();

        botService.handlePhoto(chatId, message);
        sendMessage(chatId, "📸 Фото получено. Идёт анализ...");

        CompletableFuture.runAsync(() -> {
            try {

                PhotoSize photo = message.getPhoto().stream()
                        .max(Comparator.comparing(PhotoSize::getFileSize))
                        .orElseThrow();

                GetFile getFileRequest = new GetFile(photo.getFileId());
                File telegramFile = execute(getFileRequest);

                java.io.File downloadedFile = downloadFile(
                        telegramFile,
                        new java.io.File("src/main/resources/downloads/" + chatId + ".jpg")
                );

                log.info("Фото скачано: {}", downloadedFile.getAbsolutePath());

                String rawText = botService.recognizeLicensePlate(downloadedFile);
                String extractedPlate = botService.extractLicensePlate(rawText);
                log.info("📋 OCR (локально, только лог): {}", extractedPlate);

                byte[] photoBytes = Files.readAllBytes(downloadedFile.toPath());

                handleCarPhoto(chatId, photoBytes, downloadedFile.getName());

            } catch (Exception e) {
                log.error("❌ Ошибка обработки фото", e);
                sendMessage(chatId, "❌ Ошибка анализа фото.");
            }
        });
    }

    private void handleCarPhoto(Long chatId, byte[] photoBytes, String filename) {

        try {

            Map<String, String> result =
                    fleetAiService.analyzeCar(photoBytes, filename);

            String carType = result.getOrDefault("car_type", "Unknown");
            String plate = result.getOrDefault("license_plate", "Unknown");

            sendMessage(chatId,
                    "🚗 Тип авто: " + carType +
                            "\n🔢 Номер: " + plate +
                            "\n\n✅ Фото обработано.");

        } catch (Exception e) {
            log.error("❌ Ошибка при вызове Python AI", e);
            sendMessage(chatId, "❌ Ошибка при анализе фото.");
        }
    }

    public void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("❌ Ошибка отправки сообщения", e);
        }
    }
}
