package com.fleetScan.taxiService.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AiHealthChecker {

    private final FleetAiService fleetAiService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Checking AI service availability...");
        
        boolean available = fleetAiService.isAiServiceAvailable();
        
        if (available) {
            log.info("✅ AI service is available and ready");
        } else {
            log.error("❌ AI service is NOT available at startup. " +
                    "Photo analysis will fail until the service is started. " +
                    "Please ensure the AI service is running or check docker-compose configuration.");
        }
    }
}
