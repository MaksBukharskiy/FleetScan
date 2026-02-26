package com.fleetScan.taxiService.dto;

public record AiAnalysisResult(
        String plateNumber,
        double plateConfidence,
        String condition,
        double conditionConfidence
) {
}
