package com.podcast.indexer.service;

import com.podcast.indexer.config.PodcastConfig;
import com.podcast.indexer.dto.AnswerResponse;
import com.podcast.indexer.model.EmbeddingChunk;
import com.podcast.indexer.model.Episode;
import com.podcast.indexer.repository.EmbeddingChunkRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = QuestionAnswerServiceCacheTest.TestConfig.class)
class QuestionAnswerServiceCacheTest {

    @org.springframework.beans.factory.annotation.Autowired
    private QuestionAnswerService questionAnswerService;

    @org.springframework.beans.factory.annotation.Autowired
    private OllamaService ollamaService;

    @org.springframework.beans.factory.annotation.Autowired
    private EmbeddingChunkRepository embeddingChunkRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private PodcastConfig podcastConfig;

    @Test
    void answerQuestionUsesCacheForRepeatedQuestions() {
        podcastConfig.getVector().getSearch().setTopK(1);

        Episode episode = Episode.builder()
                .id(1L)
                .title("Episode One")
                .audioUrl("https://example.com/audio.mp3")
                .build();
        EmbeddingChunk chunk = EmbeddingChunk.builder()
                .episode(episode)
                .chunkIndex(0)
                .startMs(0L)
                .endMs(1000L)
                .text("Sample transcript text.")
                .build();

        when(ollamaService.generateEmbedding("What was discussed?"))
                .thenReturn(List.of(0.1, 0.2));
        when(embeddingChunkRepository.findTopKSimilarByPodcast(eq(1L), anyString(), eq(1)))
                .thenReturn(List.of(chunk));
        when(ollamaService.generateAnswer(eq("What was discussed?"), anyString()))
                .thenReturn("Cached answer");

        AnswerResponse first = questionAnswerService.answerQuestion(1L, "What was discussed?");
        AnswerResponse second = questionAnswerService.answerQuestion(1L, "What was discussed?");

        assertThat(second.getAnswer()).isEqualTo("Cached answer");
        assertThat(second).isEqualTo(first);
        verify(ollamaService, times(1)).generateEmbedding("What was discussed?");
        verify(embeddingChunkRepository, times(1)).findTopKSimilarByPodcast(eq(1L), anyString(), eq(1));
        verify(ollamaService, times(1)).generateAnswer(eq("What was discussed?"), anyString());
    }

    @Configuration
    @EnableCaching
    static class TestConfig {
        @Bean
        PodcastConfig podcastConfig() {
            return new PodcastConfig();
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("qa-answers");
        }

        @Bean
        OllamaService ollamaService() {
            return Mockito.mock(OllamaService.class);
        }

        @Bean
        EmbeddingChunkRepository embeddingChunkRepository() {
            return Mockito.mock(EmbeddingChunkRepository.class);
        }

        @Bean
        QuestionAnswerService questionAnswerService(OllamaService ollamaService,
                                                    EmbeddingChunkRepository embeddingChunkRepository,
                                                    PodcastConfig podcastConfig) {
            return new QuestionAnswerService(ollamaService, embeddingChunkRepository, podcastConfig);
        }
    }
}
