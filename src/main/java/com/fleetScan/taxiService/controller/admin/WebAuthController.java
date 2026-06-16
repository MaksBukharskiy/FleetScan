package com.fleetScan.taxiService.controller.admin;

import com.fleetScan.taxiService.dto.WebLoginRequest;
import com.fleetScan.taxiService.dto.WebLoginResponse;
import com.fleetScan.taxiService.dto.WebSessionView;
import com.fleetScan.taxiService.dto.WebWhoAmIView;
import com.fleetScan.taxiService.domain.autopark.driver.Driver;
import com.fleetScan.taxiService.repository.autopark.DriverRepository;
import com.fleetScan.taxiService.service.security.WebAuthService;
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
@RequestMapping("/api/auth")
public class WebAuthController {

    private final WebAuthService webAuthService;
    private final DriverRepository driverRepository;

    @PostMapping("/login")
    public WebLoginResponse login(@RequestBody WebLoginRequest request) {
        return new WebLoginResponse(webAuthService.login(request.accessToken()));
    }

    @GetMapping("/me")
    public WebSessionView me(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return webAuthService.sessionInfo(authorization);
    }

    @GetMapping("/whoami")
    public WebWhoAmIView whoami(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        WebSessionView session = webAuthService.sessionInfo(authorization);
        Long chatId = webAuthService.requireChatId(authorization);
        Driver driver = driverRepository.findByChatIdAndIsActiveTrue(chatId).orElse(null);
        return new WebWhoAmIView(
                session.authenticated(),
                chatId,
                driver == null || driver.getRole() == null ? null : driver.getRole().name(),
                driver == null || driver.getFleet() == null ? null : driver.getFleet().getId(),
                driver == null || driver.getFleet() == null ? null : driver.getFleet().getName(),
                session.expiresAtEpochMillis()
        );
    }

    @DeleteMapping("/logout")
    public void logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        webAuthService.logout(authorization);
    }
}
