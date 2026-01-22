package com.podcast.indexer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    
    @Bean
    public RestClient whisperRestClient(@Value("${podcast.whisper.service.url}") String whisperUrl) {
        return RestClient.builder()
                .baseUrl(whisperUrl)
                .build();
    }
    
    @Bean
    public RestClient ollamaRestClient(@Value("${podcast.ollama.service.url}") String ollamaUrl) {
        return RestClient.builder()
                .baseUrl(ollamaUrl)
                .build();
    }
}
