package com.podcast.indexer.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.podcast.indexer.config.PodcastConfig;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OllamaService {
    
    @Qualifier("ollamaWebClient")
    private final WebClient ollamaWebClient;
    private final PodcastConfig config;
    
    public List<Double> generateEmbedding(String text) {
        try {
            EmbeddingRequest request = new EmbeddingRequest();
            request.setModel(config.getOllama().getEmbedding().getModel());
            request.setPrompt(text);
            
            EmbeddingResponse response = ollamaWebClient.post()
                    .uri("/api/embeddings")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(EmbeddingResponse.class)
                    .block();
            
            return response != null ? response.getEmbedding() : null;
        } catch (Exception e) {
            log.error("Failed to generate embedding", e);
            throw new RuntimeException("Failed to generate embedding", e);
        }
    }
    
    public String generateAnswer(String question, String context) {
        try {
            ChatRequest request = new ChatRequest();
            request.setModel(config.getOllama().getChat().getModel());
            request.setPrompt(buildPrompt(question, context));
            request.setStream(false);
            
            ChatResponse response = ollamaWebClient.post()
                    .uri("/api/generate")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ChatResponse.class)
                    .block();
            
            return response != null ? response.getResponse() : null;
        } catch (Exception e) {
            log.error("Failed to generate answer", e);
            throw new RuntimeException("Failed to generate answer", e);
        }
    }
    
    private String buildPrompt(String question, String context) {
        return "Answer the following question based only on the provided context from podcast episodes.\n\n" +
                "Context:\n" + context + "\n\n" +
                "Question: " + question + "\n\n" +
                "Answer:";
    }
    
    @Data
    public static class EmbeddingRequest {
        private String model;
        private String prompt;
    }
    
    @Data
    public static class EmbeddingResponse {
        private List<Double> embedding;
    }
    
    @Data
    public static class ChatRequest {
        private String model;
        private String prompt;
        private boolean stream;
    }
    
    @Data
    public static class ChatResponse {
        private String response;
    }
}
