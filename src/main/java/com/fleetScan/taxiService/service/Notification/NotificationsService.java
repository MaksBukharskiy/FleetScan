package com.fleetScan.taxiService.service.Notification;

import com.fleetScan.taxiService.entity.Autopark.Driver.Driver;
import com.fleetScan.taxiService.repository.Autopark.Car.CarPhotoRepository;
import com.fleetScan.taxiService.repository.Autopark.DriverRepository;
import com.fleetScan.taxiService.service.BotCommunication.FleetScanBot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationsService {

    private final DriverRepository driverRepository;
    private final CarPhotoRepository carPhotoRepository;
    private final FleetScanBot fleetScanBot;

    @Scheduled(cron = "0 0 12 * * *")
    public void remindDriver(){

        log.info("⏰ Запуск ежечасного уведомления водителей");

        List<Driver> actualDriver = driverRepository.findAll();

        for(Driver driver : actualDriver){
            fleetScanBot.sendMessage(driver.getChatId(), "\uD83D\uDD14 Привет! Пожалуйста, отправьте фото вашей машины для отчёта за сегодня.;\n");
            log.info("Отправлено уведомление водителю: {}", driver.getName());
        }

    }


}
