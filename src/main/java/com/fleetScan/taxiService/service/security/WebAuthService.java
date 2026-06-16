package com.fleetScan.taxiService.service.security;

import com.fleetScan.taxiService.domain.autopark.driver.Driver;
import com.fleetScan.taxiService.domain.autopark.driver.UserRole;
import com.fleetScan.taxiService.dto.WebSessionView;
import com.fleetScan.taxiService.repository.autopark.DriverRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class WebAuthService {

    private static final long SESSION_TTL_MILLIS = 12L * 60L * 60L * 1000L;

    private final String expectedAccessToken;
    private final Long adminChatId;
    private final DriverRepository driverRepository;
    private final Map<String, SessionData> sessions = new ConcurrentHashMap<>();

    public WebAuthService(
            DriverRepository driverRepository,
            @Value("${fleetscan.web.access-token:}") String expectedAccessToken,
            @Value("${fleetscan.web.admin-chat-id:}") String adminChatIdValue
    ) {
        this.driverRepository = driverRepository;
        this.expectedAccessToken = expectedAccessToken;
        this.adminChatId = parseAdminChatId(adminChatIdValue);
    }

    public String login(String accessToken) {
        String configuredToken = (expectedAccessToken == null || expectedAccessToken.isBlank()) ? "1234" : expectedAccessToken.trim();
        if (accessToken == null || !configuredToken.equals(accessToken.trim())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid access token");
        }

        Long effectiveAdminChatId = adminChatId != null ? adminChatId : resolveFallbackAdminChatId();
        String sessionToken = UUID.randomUUID().toString();
        sessions.put(sessionToken, new SessionData(effectiveAdminChatId, Instant.now().toEpochMilli() + SESSION_TTL_MILLIS));
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

    private Long parseAdminChatId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid fleetscan.web.admin-chat-id value", e);
        }
    }

    private Long resolveFallbackAdminChatId() {
        Long adminChatIdFromDirectory = driverRepository.findAllByRoleInAndIsActiveTrue(List.of(UserRole.ADMIN)).stream()
                .map(Driver::getChatId)
                .filter(chatId -> chatId != null)
                .findFirst()
                .orElse(null);

        if (adminChatIdFromDirectory != null) {
            return adminChatIdFromDirectory;
        }

        log.warn("Web admin chat id is not configured and no active ADMIN user with chatId found; using local fallback chatId=0");
        return 0L;
    }

    private record SessionData(Long chatId, long expiresAtEpochMillis) {
    }
}
