package com.fleetScan.taxiService.service.security;

import com.fleetScan.taxiService.domain.autopark.driver.Driver;
import com.fleetScan.taxiService.dto.DriverSessionView;
import com.fleetScan.taxiService.repository.autopark.DriverRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DriverWebAuthService {

    private static final long SESSION_TTL_MILLIS = 12L * 60L * 60L * 1000L;

    private final String expectedAccessToken;
    private final DriverRepository driverRepository;
    private final Map<String, SessionData> sessions = new ConcurrentHashMap<>();

    public DriverWebAuthService(
            @Value("${fleetscan.driver-web.access-token:}") String expectedAccessToken,
            DriverRepository driverRepository
    ) {
        this.expectedAccessToken = expectedAccessToken;
        this.driverRepository = driverRepository;
    }

    public String login(String accessToken, Long chatId) {
        if (expectedAccessToken == null || expectedAccessToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Driver web token is not configured");
        }
        if (accessToken == null || !expectedAccessToken.equals(accessToken.trim())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid access token");
        }
        if (chatId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "chatId is required");
        }

        Driver driver = driverRepository.findByChatIdAndIsActiveTrue(chatId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Driver not found or inactive"));

        String sessionToken = UUID.randomUUID().toString();
        sessions.put(sessionToken, new SessionData(driver.getId(), chatId, driver.getName(), Instant.now().toEpochMilli() + SESSION_TTL_MILLIS));
        return sessionToken;
    }

    public DriverSessionView sessionInfo(String authorizationHeader) {
        SessionData session = resolveSession(authorizationHeader);
        return new DriverSessionView(true, session.driverId(), session.driverName(), session.expiresAtEpochMillis());
    }

    public void logout(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        if (token != null) {
            sessions.remove(token);
        }
    }

    public Driver requireDriver(String authorizationHeader) {
        SessionData session = resolveSession(authorizationHeader);
        return driverRepository.findById(session.driverId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Driver session invalid"));
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

    private record SessionData(Long driverId, Long chatId, String driverName, long expiresAtEpochMillis) {
    }
}
