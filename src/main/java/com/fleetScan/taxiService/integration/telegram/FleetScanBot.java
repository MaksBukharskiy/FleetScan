package com.fleetScan.taxiService.integration.telegram;

import com.fleetScan.taxiService.domain.autopark.vehicle.DetectedVehicle;
import com.fleetScan.taxiService.domain.autopark.vehicle.VehicleStatus;
import com.fleetScan.taxiService.service.bot.BotService;
import com.fleetScan.taxiService.service.detection.DetectionProcessingService;
import com.fleetScan.taxiService.service.ai.FleetAiService;
import com.fleetScan.taxiService.service.notification.NotificationsService;
import com.fleetScan.taxiService.dto.AiAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
@RequiredArgsConstructor
public class FleetScanBot extends TelegramLongPollingBot {

    private final BotService botService;
    private final FleetAiService fleetAiService;
    private final DetectionProcessingService detectionProcessingService;
    private final NotificationsService notificationsService;

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
        try {
            sendMessage(chatId, botService.handleTextMessage(chatId, text));
        } catch (IllegalArgumentException e) {
            sendMessage(chatId, "❌ " + e.getMessage());
        }
    }

    private void handlePhoto(Update update) {

        Long chatId = update.getMessage().getChatId();
        Message message = update.getMessage();

        try {
            botService.handlePhoto(chatId, message);
        } catch (IllegalArgumentException e) {
            sendMessage(chatId, "❌ " + e.getMessage());
            return;
        }
        sendMessage(chatId, "📸 Фото получено. Идёт анализ...");

        CompletableFuture.runAsync(() -> {
            try {

                PhotoSize photo = message.getPhoto().stream()
                        .max(Comparator.comparing(PhotoSize::getFileSize))
                        .orElseThrow();

                GetFile getFileRequest = new GetFile(photo.getFileId());
                File telegramFile = execute(getFileRequest);

                Path downloadDir = Path.of("src/main/resources/downloads");
                Files.createDirectories(downloadDir);

                java.io.File downloadedFile = downloadFile(
                        telegramFile,
                        downloadDir.resolve(chatId + "-" + System.currentTimeMillis() + ".jpg").toFile()
                );

                log.info("Фото скачано: {}", downloadedFile.getAbsolutePath());

                byte[] photoBytes = Files.readAllBytes(downloadedFile.toPath());

                handleCarPhoto(chatId, photoBytes, downloadedFile.getAbsolutePath());

            } catch (Exception e) {
                log.error("❌ Ошибка обработки фото", e);
                sendMessage(chatId, "❌ Ошибка анализа фото.");
            }
        });
    }

    private void handleCarPhoto(Long chatId, byte[] photoBytes, String imagePath) {

        try {
            AiAnalysisResult ai = fleetAiService.analyzeCar(photoBytes, java.nio.file.Path.of(imagePath).getFileName().toString());
            DetectedVehicle detectedVehicle = detectionProcessingService.process(chatId, imagePath, ai);

            sendMessage(chatId,
                    "🔢 Номер: " + detectedVehicle.getPlateNumber() +
                            "\n📊 Статус: " + detectedVehicle.getStatus() +
                            "\n🎯 Confidence: " + String.format("%.2f", detectedVehicle.getConfidence()) +
                            "\n🛠 Состояние: " + detectedVehicle.getVehicleCondition() +
                            "\n\n✅ Фото обработано.");

            if (detectedVehicle.getStatus() == VehicleStatus.BLOCKED
                    || detectedVehicle.getStatus() == VehicleStatus.UNDER_REVIEW) {
                String securityText = notificationsService.buildSecurityMessage(detectedVehicle);
                notificationsService.resolveSecurityRecipients(chatId)
                        .forEach(targetChatId -> sendMessage(targetChatId, securityText));
            }

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
