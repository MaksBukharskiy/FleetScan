package com.fleetScan.taxiService.controller.admin;

import com.fleetScan.taxiService.domain.autopark.vehicle.BlackList;
import com.fleetScan.taxiService.dto.BlackListRequest;
import com.fleetScan.taxiService.dto.BlackListStatusView;
import com.fleetScan.taxiService.dto.BlackListView;
import com.fleetScan.taxiService.service.blacklist.BlackListService;
import com.fleetScan.taxiService.service.security.WebAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/blacklist")
public class AdminBlacklistController {

    private final BlackListService blackListService;
    private final WebAuthService webAuthService;

    @GetMapping
    public List<BlackListView> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Long chatId = webAuthService.requireChatId(authorization);
        return blackListService.listAll(chatId).stream().map(this::toView).toList();
    }

    @GetMapping("/status")
    public BlackListStatusView status(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String plateNumber
    ) {
        Long chatId = webAuthService.requireChatId(authorization);
        String normalized = blackListService.normalizePlate(plateNumber);
        return blackListService.findByPlate(chatId, plateNumber)
                .map(entry -> new BlackListStatusView(normalized, Boolean.TRUE.equals(entry.getIsActive()), entry.getReason()))
                .orElse(new BlackListStatusView(normalized, false, ""));
    }

    @PostMapping
    public BlackListView add(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody BlackListRequest request
    ) {
        Long chatId = webAuthService.requireChatId(authorization);
        blackListService.addPlate(chatId, request.plateNumber(), request.reason(), request.category(), request.expiresAt());
        return blackListService.findByPlate(chatId, request.plateNumber())
                .map(this::toView)
                .orElseThrow(() -> new IllegalStateException("Black list entry was not created"));
    }

    @DeleteMapping("/{plateNumber}")
    public BlackListStatusView remove(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String plateNumber
    ) {
        Long chatId = webAuthService.requireChatId(authorization);
        blackListService.removePlate(chatId, plateNumber);
        String normalized = blackListService.normalizePlate(plateNumber);
        return new BlackListStatusView(normalized, false, "");
    }

    private BlackListView toView(BlackList entity) {
        return new BlackListView(
                entity.getId(),
                entity.getPlateNumber(),
                entity.getCreatedByChatId(),
                Boolean.TRUE.equals(entity.getIsActive()),
                entity.getReason(),
                entity.getCategory(),
                entity.getExpiresAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
