package com.fleetScan.taxiService.service.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One-time web login codes issued by the Telegram bot (/web).
 * A code proves the person controls that Telegram account, so the web UI
 * no longer needs shared passwords or a typed-in chat id.
 */
@Service
public class LoginCodeService {

    static final long CODE_TTL_MILLIS = 5L * 60L * 1000L;
    // No 0/O/1/I/L to avoid typos. 31^8 ≈ 8.5e11 combinations.
    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Pending> codes = new ConcurrentHashMap<>();

    public String issue(Long chatId) {
        purgeExpired();
        codes.values().removeIf(p -> p.chatId().equals(chatId));

        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
            code = sb.toString();
        } while (codes.containsKey(code));

        codes.put(code, new Pending(chatId, Instant.now().toEpochMilli() + CODE_TTL_MILLIS));
        return code.substring(0, 4) + "-" + code.substring(4);
    }

    /** Consumes the code (single use) and returns the Telegram chat id it was issued to. */
    public Long redeem(String rawCode) {
        String code = normalize(rawCode);
        if (code.length() != CODE_LENGTH) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid login code");
        }
        Pending pending = codes.remove(code);
        if (pending == null || pending.expiresAtEpochMillis() < Instant.now().toEpochMilli()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired login code");
        }
        return pending.chatId();
    }

    static String normalize(String rawCode) {
        return rawCode == null ? "" : rawCode.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }

    private void purgeExpired() {
        long now = Instant.now().toEpochMilli();
        codes.values().removeIf(p -> p.expiresAtEpochMillis() < now);
    }

    private record Pending(Long chatId, long expiresAtEpochMillis) {
    }
}
