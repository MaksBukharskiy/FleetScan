package com.fleetScan.taxiService.service.security;

import com.fleetScan.taxiService.domain.autopark.driver.Driver;
import com.fleetScan.taxiService.domain.autopark.driver.UserRole;
import com.fleetScan.taxiService.dto.WebSessionView;
import com.fleetScan.taxiService.repository.autopark.DriverRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class WebAuthService {

    private static final long SESSION_TTL_MILLIS = 12L * 60L * 60L * 1000L;

    private static final Set<UserRole> WEB_ADMIN_ROLES = EnumSet.of(UserRole.ADMIN, UserRole.OBSERVER);

    private final DriverRepository driverRepository;
    private final LoginCodeService loginCodeService;
    private final Map<String, SessionData> sessions = new ConcurrentHashMap<>();

    public WebAuthService(DriverRepository driverRepository, LoginCodeService loginCodeService) {
        this.driverRepository = driverRepository;
        this.loginCodeService = loginCodeService;
    }

    /** Log in with a one-time code from the Telegram bot (/web). Only fleet admins and observers. */
    public String login(String code) {
        Long chatId = loginCodeService.redeem(code);
        Driver user = driverRepository.findByChatIdAndIsActiveTrue(chatId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found or inactive"));
        if (!WEB_ADMIN_ROLES.contains(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin panel is available to fleet admins and observers only");
        }

        String sessionToken = UUID.randomUUID().toString();
        sessions.put(sessionToken, new SessionData(chatId, Instant.now().toEpochMilli() + SESSION_TTL_MILLIS));
        log.info("Web admin login: chatId={}, role={}", chatId, user.getRole());
        return sessionToken;
    }

    public WebSessionView sessionInfo(String authorizationHeader) {
        SessionData session = resolveSession(authorizationHeader);
        return new WebSessionView(true, session.expiresAtEpochMillis());
    }

    public void logout(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        if (token != null) {
            sessions.remove(token);
        }
    }

    public Long requireChatId(String authorizationHeader) {
        SessionData session = resolveSession(authorizationHeader);
        return session.chatId();
    }

    private SessionData resolveSession(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        if (token == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token");
        }

        SessionData session = sessions.get(token);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid session");
        }
        if (session.expiresAtEpochMillis() < Instant.now().toEpochMilli()) {
            sessions.remove(token);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session expired");
        }
        return session;
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        String prefix = "Bearer ";
        if (!authorizationHeader.startsWith(prefix)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authorization header");
        }
        return authorizationHeader.substring(prefix.length()).trim();
    }

    private record SessionData(Long chatId, long expiresAtEpochMillis) {
    }
}
