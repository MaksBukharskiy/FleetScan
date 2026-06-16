package com.fleetScan.taxiService.domain.autopark.car;

import lombok.Getter;

@Getter
public enum PhotoType {
    FRONT("Передняя часть"),
    REAR("Задняя часть"),
    INTERIOR("Салон"),
    DASHBOARD("Приборная панель"),
    OTHER("Другое");

    private final String description;

    PhotoType(String description) {
        this.description = description;
    }

    public static PhotoType fromDescription(String text) {
        if (text == null) return OTHER;
        String lower = text.toLowerCase();
        if (lower.contains("перед") || lower.contains("front")) return FRONT;
        if (lower.contains("зад") || lower.contains("rear")) return REAR;
        if (lower.contains("салон") || lower.contains("interior")) return INTERIOR;
        if (lower.contains("панель") || lower.contains("dashboard") || lower.contains("прибор")) return DASHBOARD;
        return OTHER;
    }
}
