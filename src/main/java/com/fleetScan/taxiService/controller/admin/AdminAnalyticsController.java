package com.fleetScan.taxiService.controller.admin;

import com.fleetScan.taxiService.domain.autopark.vehicle.VehicleStatus;
import com.fleetScan.taxiService.service.analytics.AnalyticsService;
import com.fleetScan.taxiService.dto.AnalyticsSummary;
import com.fleetScan.taxiService.dto.DetectionView;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
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

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel(
            @RequestHeader("X-Chat-Id") Long chatId,
            @RequestParam(required = false) String plate,
            @RequestParam(required = false) VehicleStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        LocalDateTime from = fromDate == null ? LocalDate.now().minusDays(7).atStartOfDay() : fromDate.atStartOfDay();
        LocalDateTime to = toDate == null ? LocalDate.now().atTime(LocalTime.MAX) : toDate.atTime(LocalTime.MAX);
        List<DetectionView> rows = analyticsService.detections(chatId, plate, status, from, to);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Detections");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Номер авто", "Статус", "Уверенность", "Состояние", "Причина решения", "Дата обнаружения"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (DetectionView row : rows) {
                Row dataRow = sheet.createRow(rowNum++);
                dataRow.createCell(0).setCellValue(row.id());
                dataRow.createCell(1).setCellValue(row.plateNumber());
                dataRow.createCell(2).setCellValue(row.status().name());
                dataRow.createCell(3).setCellValue(row.confidence());
                dataRow.createCell(4).setCellValue(row.condition());
                dataRow.createCell(5).setCellValue(row.decisionReason());
                dataRow.createCell(6).setCellValue(row.detectedAt().toString());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            outputStream.flush();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"detections.xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(outputStream.toByteArray());

        } catch (Exception e) {
            throw new RuntimeException("Ошибка экспорта в Excel", e);
        }
    }
}
