package com.fleetScan.taxiService.service.ai;

import com.fleetScan.taxiService.dto.AiAnalysisResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.core.io.ByteArrayResource;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;


@Service
public class FleetAiService {
    private final WebClient webClient;

    public FleetAiService(@Value("${fleetscan.ai.base-url:http://localhost:8000}") String aiBaseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(aiBaseUrl)
                .build();
    }

    public AiAnalysisResult analyzeCar(byte[] photoBytes, String filename) {

        ByteArrayResource resource = new ByteArrayResource(photoBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);

        Map<String, Object> response = webClient.post()
                .uri("/analyze")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        String plate = asString(response, "license_plate", "UNKNOWN");
        double plateConfidence = asDouble(response, "confidence", 0.0);
        String condition = asString(response, "condition", "unknown");
        double conditionConfidence = asDouble(response, "condition_confidence", 0.0);

        return new AiAnalysisResult(plate, plateConfidence, condition, conditionConfidence);
    }

    private String asString(Map<String, Object> map, String key, String defaultValue) {
        if (map == null || !map.containsKey(key) || map.get(key) == null) {
            return defaultValue;
        }
        return String.valueOf(map.get(key));
    }

    private double asDouble(Map<String, Object> map, String key, double defaultValue) {
        if (map == null || !map.containsKey(key) || map.get(key) == null) {
            return defaultValue;
        }
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
