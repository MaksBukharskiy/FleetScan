package com.fleetScan.taxiService.dto;

public record AiAnalysisResult(
        String plateNumber,
        double plateConfidence,
        String condition,
        double conditionConfidence,
        String damageMethod,
        String damageSeverity,
        String damageSummary
) {
    public AiAnalysisResult(String plateNumber, double plateConfidence, String condition, double conditionConfidence) {
        this(plateNumber, plateConfidence, condition, conditionConfidence, null, null, null);
    }
}
