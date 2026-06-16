package com.fleetScan.taxiService.controller.admin;

import com.fleetScan.taxiService.dto.DetectionUpdateRequest;
import com.fleetScan.taxiService.dto.DetectionView;
import com.fleetScan.taxiService.service.analytics.IncidentService;
import com.fleetScan.taxiService.service.security.WebAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/incidents")
public class AdminIncidentController {

    private final IncidentService incidentService;
    private final WebAuthService webAuthService;

    @GetMapping
    public List<DetectionView> queue(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        Long chatId = webAuthService.requireChatId(authorization);
        return incidentService.queue(chatId);
    }

    @PutMapping("/{detectionId}")
    public DetectionView update(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable Long detectionId,
            @RequestBody DetectionUpdateRequest request
    ) {
        Long chatId = webAuthService.requireChatId(authorization);
        return incidentService.update(chatId, detectionId, request);
    }
}
