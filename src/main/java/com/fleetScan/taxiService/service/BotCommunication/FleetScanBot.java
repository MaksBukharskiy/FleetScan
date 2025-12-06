package com.fleetScan.taxiService.service.BotCommunication;

import com.fleetScan.taxiService.service.Bot.BotService;
import net.sourceforge.tess4j.Tesseract;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.concurrent.CompletableFuture;

import static java.awt.SystemColor.text;

@Component
@Slf4j
@RequiredArgsConstructor
public class FleetScanBot extends TelegramLongPollingBot {

    private final BotService botService;

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
        Long chatIdForError = null;

        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                final Long chatId = update.getMessage().getChatId();
                String text = update.getMessage().getText();

                String state = botService.getUserState(chatId);

                if ("AWAITING_FLEET_NAME".equals(state)) {
                    String response = botService.createNewFleet(chatId, text);
                    sendMessage(chatId, response);
                    return;
                }

                if ("AWAITING_DRIVER_NAME".equals(state)) {
                    String response = botService.addNewDriver(chatId, text);
                    sendMessage(chatId, response);
                    return;
                }

                if (text.startsWith("/start")) {
                    String[] parts = text.split(" ", 2);
                    if (parts.length > 1) {
                        String response = botService.handleInviteLink(chatId, parts[1]);
                        sendMessage(chatId, response);
                    } else {
                        String response = botService.handleStartCommand(chatId);
                        sendMessage(chatId, response);
                    }
                    return;
                }

                if ("/add_driver".equals(text)) {
                    String response = botService.startToAddDriver(chatId);
                    sendMessage(chatId, response);
                    return;
                }

                sendMessage(chatId, "Неизвестная команда. Используйте /start");

            }

            else if (update.hasMessage() && update.getMessage().hasPhoto()) {
                final Long chatId = update.getMessage().getChatId();
                Message message = update.getMessage();

                botService.handlePhoto(chatId, message);
                sendMessage(chatId, "✅ Фото загружено! Идет анализ...");

                CompletableFuture.runAsync(() -> {
                    try {
                        PhotoSize photo = message.getPhoto().stream()
                                .max(Comparator.comparing(PhotoSize::getFileSize))
                                .orElseThrow();

                        GetFile getFileRequest = new GetFile();
                        getFileRequest.setFileId(photo.getFileId());
                        File telegramFile = execute(getFileRequest);


                        java.io.File downloadedFile = downloadFile(
                                telegramFile,
                                new java.io.File("src/main/resources/downloads/" + chatId + ".jpg")
                        );

                        log.info("Фото скачано: {}", downloadedFile.getAbsolutePath());

                        String number = botService.recognizeLicensePlate(downloadedFile);
                        sendMessage(chatId, "🔍 Распознан номер: **" + number + "**");

                    } catch (Exception e) {
                        log.error("Ошибка при обработке фото", e);
                        sendMessage(chatId, "❌ Не удалось распознать номер.");
                    }
                });
            }

            else {
                log.warn("❌ Получено сообщение без текста");
            }

        }
        catch (Exception e) {
            log.error("❌ Ошибка при обработке сообщения", e);

            if (chatIdForError != null) {
                sendMessage(chatIdForError, "❌ Произошла ошибка. Попробуйте позже.");
            }
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