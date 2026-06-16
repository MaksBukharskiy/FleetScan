package com.fleetScan.taxiService.controller.admin;

import com.fleetScan.taxiService.dto.DriverCreateRequest;
import com.fleetScan.taxiService.dto.DriverPhotoView;
import com.fleetScan.taxiService.dto.DriverUpdateRequest;
import com.fleetScan.taxiService.dto.DriverView;
import com.fleetScan.taxiService.dto.DetectionView;
import com.fleetScan.taxiService.dto.PhotoContent;
import com.fleetScan.taxiService.service.driver.DriverManagementService;
import com.fleetScan.taxiService.service.security.WebAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/drivers")
public class AdminDriverController {

    private final DriverManagementService driverManagementService;
    private final WebAuthService webAuthService;

    @GetMapping
    public List<DriverView> list(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        Long chatId = webAuthService.requireChatId(authorization);
        return driverManagementService.list(chatId);
    }

    @PostMapping
    public DriverView create(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody DriverCreateRequest request
    ) {
        Long chatId = webAuthService.requireChatId(authorization);
        return driverManagementService.create(chatId, request);
    }

    @PutMapping("/{driverId}")
    public DriverView update(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable Long driverId,
            @RequestBody DriverUpdateRequest request
    ) {
        Long chatId = webAuthService.requireChatId(authorization);
        return driverManagementService.update(chatId, driverId, request);
    }

    @DeleteMapping("/{driverId}")
    public void delete(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable Long driverId
    ) {
        Long chatId = webAuthService.requireChatId(authorization);
        driverManagementService.delete(chatId, driverId);
    }

    @GetMapping("/{driverId}/photos")
    public List<DriverPhotoView> photos(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable Long driverId
    ) {
        Long chatId = webAuthService.requireChatId(authorization);
        return driverManagementService.photos(chatId, driverId);
    }

    @GetMapping("/{driverId}/photos/{photoId}/content")
    public ResponseEntity<byte[]> photoContent(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable Long driverId,
            @PathVariable Long photoId
    ) {
        Long chatId = webAuthService.requireChatId(authorization);
        PhotoContent content = driverManagementService.photoContent(chatId, driverId, photoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(content.bytes());
    }

    @GetMapping("/{driverId}/detections")
    public List<DetectionView> detections(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable Long driverId
    ) {
        Long chatId = webAuthService.requireChatId(authorization);
        return driverManagementService.detections(chatId, driverId);
    }
}
