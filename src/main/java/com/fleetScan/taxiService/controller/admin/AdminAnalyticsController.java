package com.fleetScan.taxiService.controller.admin;

import com.fleetScan.taxiService.domain.autopark.vehicle.VehicleStatus;
import com.fleetScan.taxiService.dto.AnalyticsSummary;
import com.fleetScan.taxiService.dto.DetectionView;
import com.fleetScan.taxiService.service.analytics.AnalyticsService;
import com.fleetScan.taxiService.service.security.WebAuthService;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;
    private final WebAuthService webAuthService;

    @GetMapping("/summary")
    public AnalyticsSummary summary(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        Long chatId = webAuthService.requireChatId(authorization);
        LocalDateTime from = fromDate == null ? LocalDate.now().minusDays(7).atStartOfDay() : fromDate.atStartOfDay();
        LocalDateTime to = toDate == null ? LocalDate.now().atTime(LocalTime.MAX) : toDate.atTime(LocalTime.MAX);
        return analyticsService.summary(chatId, from, to);
    }

    @GetMapping("/detections")
    public List<DetectionView> detections(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam(required = false) String plate,
            @RequestParam(required = false) VehicleStatus status,
            @RequestParam(required = false) String decisionReason,
            @RequestParam(required = false) Double confidenceFrom,
            @RequestParam(required = false) Double confidenceTo,
            @RequestParam(required = false) Long driverChatId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        Long chatId = webAuthService.requireChatId(authorization);
        LocalDateTime from = fromDate == null ? LocalDate.now().minusDays(7).atStartOfDay() : fromDate.atStartOfDay();
        LocalDateTime to = toDate == null ? LocalDate.now().atTime(LocalTime.MAX) : toDate.atTime(LocalTime.MAX);
        return analyticsService.detections(chatId, plate, status, decisionReason, confidenceFrom, confidenceTo, driverChatId, from, to);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam(required = false) String plate,
            @RequestParam(required = false) VehicleStatus status,
            @RequestParam(required = false) String decisionReason,
            @RequestParam(required = false) Double confidenceFrom,
            @RequestParam(required = false) Double confidenceTo,
            @RequestParam(required = false) Long driverChatId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        Long chatId = webAuthService.requireChatId(authorization);
        LocalDateTime from = fromDate == null ? LocalDate.now().minusDays(7).atStartOfDay() : fromDate.atStartOfDay();
        LocalDateTime to = toDate == null ? LocalDate.now().atTime(LocalTime.MAX) : toDate.atTime(LocalTime.MAX);
        List<DetectionView> rows = analyticsService.detections(chatId, plate, status, decisionReason, confidenceFrom, confidenceTo, driverChatId, from, to);

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

    @GetMapping("/export/incidents")
    public ResponseEntity<byte[]> exportIncidentsCsv(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        Long chatId = webAuthService.requireChatId(authorization);
        LocalDateTime from = fromDate == null ? LocalDate.now().minusDays(30).atStartOfDay() : fromDate.atStartOfDay();
        LocalDateTime to = toDate == null ? LocalDate.now().atTime(LocalTime.MAX) : toDate.atTime(LocalTime.MAX);
        List<DetectionView> rows = analyticsService.incidentRows(chatId, from, to);

        StringBuilder csv = new StringBuilder("id,plate,status,confidence,condition,decisionReason,detectedAt\n");
        for (DetectionView row : rows) {
            csv.append(row.id()).append(',')
                    .append(csvSafe(row.plateNumber())).append(',')
                    .append(csvSafe(row.status() == null ? "" : row.status().name())).append(',')
                    .append(row.confidence()).append(',')
                    .append(csvSafe(row.condition())).append(',')
                    .append(csvSafe(row.decisionReason())).append(',')
                    .append(csvSafe(row.detectedAt() == null ? "" : row.detectedAt().toString()))
                    .append('\n');
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"incidents.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=utf-8"))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/export/driver-compliance")
    public ResponseEntity<byte[]> exportDriverComplianceCsv(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        Long chatId = webAuthService.requireChatId(authorization);
        List<AnalyticsService.DriverComplianceRow> rows = analyticsService.driverCompliance(chatId);

        StringBuilder csv = new StringBuilder("driverId,driverName,chatId,fleetName,role,totalDetections,blockedCount,underReviewCount,complianceStatus\n");
        for (AnalyticsService.DriverComplianceRow row : rows) {
            csv.append(row.driverId()).append(',')
                    .append(csvSafe(row.driverName())).append(',')
                    .append(row.chatId() == null ? "" : row.chatId()).append(',')
                    .append(csvSafe(row.fleetName())).append(',')
                    .append(csvSafe(row.role())).append(',')
                    .append(row.totalDetections()).append(',')
                    .append(row.blockedCount()).append(',')
                    .append(row.underReviewCount()).append(',')
                    .append(csvSafe(row.complianceStatus()))
                    .append('\n');
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"driver-compliance.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=utf-8"))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String csvSafe(String value) {
        String source = value == null ? "" : value;
        return '"' + source.replace("\"", "\"\"") + '"';
    }
}
