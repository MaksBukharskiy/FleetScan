package com.fleetScan.taxiService.service.Notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotifivationsService {

    @Scheduled(cron = "0 0 * * * *")


}
