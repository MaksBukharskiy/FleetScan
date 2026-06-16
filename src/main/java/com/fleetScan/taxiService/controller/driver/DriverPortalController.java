package com.fleetScan.taxiService.controller.driver;

import com.fleetScan.taxiService.domain.autopark.car.PhotoType;
import com.fleetScan.taxiService.dto.DetectionView;
import com.fleetScan.taxiService.dto.DriverPhotoUploadRequest;
import com.fleetScan.taxiService.dto.DriverPhotoView;
import com.fleetScan.taxiService.dto.DriverProfileView;
import com.fleetScan.taxiService.dto.PhotoContent;
import com.fleetScan.taxiService.service.driver.DriverPortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/driver")
public class DriverPortalController {

    private final DriverPortalService driverPortalService;

    @GetMapping("/profile")
    public DriverProfileView profile(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return driverPortalService.profile(authorization);
    }

    @GetMapping("/photos")
    public List<DriverPhotoView> photos(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return driverPortalService.photos(authorization);
    }

    @PostMapping(value = "/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DriverPhotoView upload(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "photoType", required = false) String photoType,
            @RequestPart(value = "note", required = false) String note
    ) {
        PhotoType type = photoType == null || photoType.isBlank() ? null : PhotoType.valueOf(photoType.trim().toUpperCase());
        return driverPortalService.upload(authorization, file, new DriverPhotoUploadRequest(type, note));
    }

    @GetMapping("/detections")
    public List<DetectionView> detections(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return driverPortalService.detections(authorization);
    }

    @GetMapping("/photos/{photoId}/content")
    public ResponseEntity<byte[]> photoContent(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable Long photoId
    ) {
        PhotoContent content = driverPortalService.photoContent(authorization, photoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(content.bytes());
    }

    @GetMapping("/detections/{detectionId}/photo")
    public ResponseEntity<byte[]> detectionPhotoContent(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable Long detectionId
    ) {
        PhotoContent content = driverPortalService.detectionPhotoContent(authorization, detectionId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(content.bytes());
    }
}
