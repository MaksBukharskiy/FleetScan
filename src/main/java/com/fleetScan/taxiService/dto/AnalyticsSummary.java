package com.fleetScan.taxiService.dto;

public record AnalyticsSummary(
        long totalDetections,
        long activeCount,
        long blockedCount,
        long underReviewCount
) {
}
