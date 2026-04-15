package com.esprit.microservice.medicalmediaservice.services;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class AIClassificationService {

    private final WebClient webClient = WebClient.create();

    private final String API_URL =
            "https://api-inference.huggingface.co/models/facebook/bart-large-mnli";

    // ⚠️ TEMPORAIRE (change après)
    private final String API_KEY = "Bearer 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";

    public String classify(String text) {

        try {
            Map<String, Object> body = Map.of(
                    "inputs", text,
                    "parameters", Map.of(
                            "candidate_labels",
                            List.of("ORDONNANCE", "BILAN", "RADIO", "COMPTE_RENDU", "CONSENTEMENT")
                    )
            );

            Map response = webClient.post()
                    .uri(API_URL)
                    .header("Authorization", API_KEY)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<String> labels = (List<String>) response.get("labels");

            return labels.get(0);

        } catch (Exception e) {
            e.printStackTrace();
            return "AUTRE";
        }
    }
}