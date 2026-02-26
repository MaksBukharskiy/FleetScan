package com.fleetScan.taxiService.controller.admin;

import com.fleetScan.taxiService.domain.autopark.vehicle.VehicleStatus;
import com.fleetScan.taxiService.service.analytics.AnalyticsService;
import com.fleetScan.taxiService.dto.AnalyticsSummary;
import com.fleetScan.taxiService.dto.DetectionView;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/summary")
    public AnalyticsSummary summary(
            @RequestHeader("X-Chat-Id") Long chatId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        LocalDateTime from = fromDate == null ? LocalDate.now().minusDays(7).atStartOfDay() : fromDate.atStartOfDay();
        LocalDateTime to = toDate == null ? LocalDate.now().atTime(LocalTime.MAX) : toDate.atTime(LocalTime.MAX);
        return analyticsService.summary(chatId, from, to);
    }

    @GetMapping("/detections")
    public List<DetectionView> detections(
            @RequestHeader("X-Chat-Id") Long chatId,
            @RequestParam(required = false) String plate,
            @RequestParam(required = false) VehicleStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        LocalDateTime from = fromDate == null ? LocalDate.now().minusDays(7).atStartOfDay() : fromDate.atStartOfDay();
        LocalDateTime to = toDate == null ? LocalDate.now().atTime(LocalTime.MAX) : toDate.atTime(LocalTime.MAX);
        return analyticsService.detections(chatId, plate, status, from, to);
    }
}
