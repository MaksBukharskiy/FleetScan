package com.fleetScan.taxiService.controller.driver;

import com.fleetScan.taxiService.dto.DriverLoginRequest;
import com.fleetScan.taxiService.dto.DriverLoginResponse;
import com.fleetScan.taxiService.dto.DriverSessionView;
import com.fleetScan.taxiService.service.security.DriverWebAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/driver/auth")
public class DriverAuthController {

    private final DriverWebAuthService driverWebAuthService;

    @PostMapping("/login")
    public DriverLoginResponse login(@RequestBody DriverLoginRequest request) {
        return new DriverLoginResponse(driverWebAuthService.login(request.accessToken(), request.chatId()));
    }

    @GetMapping("/me")
    public DriverSessionView me(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return driverWebAuthService.sessionInfo(authorization);
    }

    @DeleteMapping("/logout")
    public void logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        driverWebAuthService.logout(authorization);
    }
}
