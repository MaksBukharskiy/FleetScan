package com.fleetScan.taxiService.service;

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

    private final BotService botService;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.name}")
    private String botName;

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                Long chatId = update.getMessage().getChatId();
                String text = update.getMessage().getText();

                if (text.startsWith("/start ") || text.contains(" ")){
                    String[] parts = text.split(" ");
                    if (parts.length > 1) {
                        String response = botService.handleInviteLink(chatId, parts[1]);
                        sendMessage(chatId, response);
                        return;
                    }
                }
            }

        }
        catch (Exception e) {}
    }

}