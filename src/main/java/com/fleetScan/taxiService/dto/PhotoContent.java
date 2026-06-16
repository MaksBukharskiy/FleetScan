package com.fleetScan.taxiService.dto;

public record PhotoContent(
        byte[] bytes,
        String contentType
) {
}
