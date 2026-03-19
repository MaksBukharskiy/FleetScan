package com.fleetScan.taxiService.service.ai;

import com.fleetScan.taxiService.dto.AiAnalysisResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;
import java.time.Duration;


@Service
@Slf4j
public class FleetAiService {
    private final WebClient webClient;
    private final String aiBaseUrl;
    private final int aiTimeoutSeconds;

    public FleetAiService(
            @Value("${fleetscan.ai.base-url:http://localhost:8000}") String aiBaseUrl,
            @Value("${fleetscan.ai.timeout-seconds:120}") int aiTimeoutSeconds
    ) {
        this.aiBaseUrl = aiBaseUrl;
        this.aiTimeoutSeconds = aiTimeoutSeconds;
        this.webClient = WebClient.builder()
                .baseUrl(aiBaseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
        log.info("AI Service configured with base URL: {}", aiBaseUrl);
        log.info("AI Service timeout configured: {} seconds", aiTimeoutSeconds);
    }

    public AiAnalysisResult analyzeCar(byte[] photoBytes, String filename) {
        log.debug("Starting AI analysis for file: {}", filename);

        ByteArrayResource resource = new ByteArrayResource(photoBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);

        try {
            Map<String, Object> response = webClient.post()
                    .uri("/analyze")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(aiTimeoutSeconds))
                    .block();

            if (response == null) {
                log.error("AI service returned null response");
                return new AiAnalysisResult("UNKNOWN", 0.0, "error", 0.0);
            }

            String plate = asString(response, "license_plate", "UNKNOWN");
            double plateConfidence = asDouble(response, "confidence", 0.0);
            String condition = asString(response, "condition", "unknown");
            double conditionConfidence = asDouble(response, "condition_confidence", 0.0);

            log.info("AI analysis complete - Plate: {}, Confidence: {:.2f}, Condition: {}", 
                    plate, plateConfidence, condition);

            return new AiAnalysisResult(plate, plateConfidence, condition, conditionConfidence);

        } catch (Exception e) {
            log.error("AI service call failed. URL: {}/analyze. Error: {}", aiBaseUrl, e.getMessage());
            log.debug("Full exception:", e);
            return new AiAnalysisResult("ERROR", 0.0, "service_unavailable", 0.0);
        }
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

    public boolean isAiServiceAvailable() {
        try {
            String response = webClient.get()
                    .uri("/")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return response != null;
        } catch (Exception e) {
            log.warn("AI service health check failed: {}", e.getMessage());
            return false;
        }
    }
}
