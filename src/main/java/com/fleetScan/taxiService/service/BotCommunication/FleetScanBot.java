package com.fleetScan.taxiService.service.BotCommunication;

import com.fleetScan.taxiService.service.Bot.BotService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class FleetScanBot extends TelegramLongPollingBot {

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    private final BotService botService;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.name}")
    private String botName;

    @Override
    public void onUpdateReceived(Update update) {
        Long chatId = null;

        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                chatId = update.getMessage().getChatId();
                String text = update.getMessage().getText();

                if (text.startsWith("/start ") || text.contains(" ")) {
                    String[] parts = text.split(" ", 2);
                    if (parts.length > 1) {
                        String response = botService.handleInviteLink(chatId, parts[1]);
                        sendMessage(chatId, response);
                        return;
                    }
                }

                String response = botService.handleMessage(chatId, text);
                sendMessage(chatId, response);

            } else {
                log.warn("❌ Получено сообщение без текста");
            }

        } catch (Exception e) {
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

        try{
            execute(message);
        } catch (TelegramApiException e){
            log.error("❌ Ошибка отправки сообщения", e);
        }

    }
}