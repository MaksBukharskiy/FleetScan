package com.fleetScan.taxiService.service.Bot;

import com.fleetScan.taxiService.service.BotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

@Component
@Slf4j
public class FleetScanBot extends TelegramLongPollingBot {

    private final BotService botService;

    public FleetScanBot(BotService botService) {
        this.botService = botService;
    }

}
