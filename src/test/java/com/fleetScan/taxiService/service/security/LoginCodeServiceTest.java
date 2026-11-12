package com.fleetScan.taxiService.service.security;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginCodeServiceTest {

    private final LoginCodeService service = new LoginCodeService();

    @Test
    void issuedCodeRedeemsToChatIdOnce() {
        String code = service.issue(42L);
        assertTrue(code.matches("[A-Z2-9]{4}-[A-Z2-9]{4}"), code);
        assertEquals(42L, service.redeem(code));
        assertThrows(ResponseStatusException.class, () -> service.redeem(code));
    }

    @Test
    void codeIsCaseAndDashInsensitive() {
        String code = service.issue(7L);
        assertEquals(7L, service.redeem(code.replace("-", "").toLowerCase()));
    }

    @Test
    void newCodeInvalidatesPreviousOneForSameUser() {
        String first = service.issue(1L);
        String second = service.issue(1L);
        assertThrows(ResponseStatusException.class, () -> service.redeem(first));
        assertEquals(1L, service.redeem(second));
    }

    @Test
    void garbageIsRejected() {
        assertThrows(ResponseStatusException.class, () -> service.redeem(null));
        assertThrows(ResponseStatusException.class, () -> service.redeem("1234"));
        assertThrows(ResponseStatusException.class, () -> service.redeem("AAAA-BBBB"));
    }
}
