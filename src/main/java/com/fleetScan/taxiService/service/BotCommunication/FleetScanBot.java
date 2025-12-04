package com.fleetScan.taxiService.service.BotCommunication;

import com.fleetScan.taxiService.service.Bot.BotService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

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
        Long chatId = null;

        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                chatId = update.getMessage().getChatId();
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

                chatId = update.getMessage().getChatId();
                Message message = update.getMessage();

                botService.handlePhoto(chatId, message);

                sendMessage(chatId, "✅ Фото загружено! Номер будет распознан через 5 секунд...");

                Long finalChatId = chatId;
                CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(5000);
                        String number = botService.recognizeLicensePlate(null);
                        sendMessage(finalChatId, "🔍 Распознан номер: ⎜" + number + "⎜");
                    } catch (Exception e) {
                        log.error("Ошибка при отправке результата OCR", e);
                    }
                });

            }

            else {
                log.warn("❌ Получено сообщение без текста");
            }

        }
        catch (Exception e) {
            log.error("❌ Ошибка при обработке сообщения", e);

            if (chatId != null) {
                sendMessage(chatId, "❌ Произошла ошибка. Попробуйте позже.");
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