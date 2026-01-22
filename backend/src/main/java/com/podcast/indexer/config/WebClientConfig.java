package com.podcast.indexer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    
    @Bean
    public WebClient whisperWebClient(@Value("${podcast.whisper.service.url}") String whisperUrl) {
        return WebClient.builder()
                .baseUrl(whisperUrl)
                .build();
    }
    
    @Bean
    public WebClient ollamaWebClient(@Value("${podcast.ollama.service.url}") String ollamaUrl) {
        return WebClient.builder()
                .baseUrl(ollamaUrl)
                .build();
    }
}
