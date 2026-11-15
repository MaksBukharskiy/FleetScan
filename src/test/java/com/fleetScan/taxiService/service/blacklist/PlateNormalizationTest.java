package com.fleetScan.taxiService.service.blacklist;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlateNormalizationTest {

    private final BlackListService service = new BlackListService(null, null);

    @Test
    void uzbekPlatesAreStrippedOfSpaces() {
        assertEquals("01A123BC", service.normalizePlate("01 A 123 BC"));
        assertEquals("01123ABC", service.normalizePlate("01 123 abc"));
    }

    @Test
    void cyrillicLookalikesBecomeLatinSoOcrMatches() {
        assertEquals("A123BC777", service.normalizePlate("А123ВС777"));
        assertEquals("", service.normalizePlate(null));
    }
}
