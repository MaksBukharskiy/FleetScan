package com.fleetScan.taxiService.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.core.io.ByteArrayResource;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;

@Service
public class FleetAiService {
    private final WebClient webClient;

    public FleetAiService() {
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:8000") // адрес твоего FastAPI
                .build();
    }

    public Map<String, String> analyzeCar(byte[] photoBytes, String filename) {

        ByteArrayResource resource = new ByteArrayResource(photoBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);

        Map<String, String> response = webClient.post()
                .uri("/analyze")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return response;
    }
}
