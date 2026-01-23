package com.podcast.indexer.service;

import com.podcast.indexer.config.PodcastConfig;
import com.podcast.indexer.dto.AnswerResponse;
import com.podcast.indexer.model.EmbeddingChunk;
import com.podcast.indexer.model.Episode;
import com.podcast.indexer.repository.EmbeddingChunkRepository;
import com.podcast.indexer.util.EmbeddingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionAnswerService {
    
    private final OllamaService ollamaService;
    private final EmbeddingChunkRepository embeddingChunkRepository;
    private final PodcastConfig config;
    
    public AnswerResponse answerQuestion(Long podcastId, String question) {
        // Generate embedding for the question
        List<Double> questionEmbedding = ollamaService.generateEmbedding(question);
        String embeddingStr = EmbeddingUtils.convertEmbeddingToString(questionEmbedding);
        
        // Retrieve top-k similar chunks
        int topK = config.getVector().getSearch().getTopK();
        List<EmbeddingChunk> relevantChunks = embeddingChunkRepository.findTopKSimilarByPodcast(
                podcastId, embeddingStr, topK);
        
        if (relevantChunks.isEmpty()) {
            return AnswerResponse.builder()
                    .answer("I don't have enough information to answer this question.")
                    .citations(new ArrayList<>())
                    .build();
        }
        
        // Build context from chunks
        StringBuilder contextBuilder = new StringBuilder();
        List<AnswerResponse.Citation> citations = new ArrayList<>();
        
        for (EmbeddingChunk chunk : relevantChunks) {
            Episode episode = chunk.getEpisode();
            
            contextBuilder.append("From episode \"")
                    .append(episode.getTitle())
                    .append("\" (")
                    .append(formatTimestamp(chunk.getStartMs()))
                    .append(" - ")
                    .append(formatTimestamp(chunk.getEndMs()))
                    .append("): ")
                    .append(chunk.getText())
                    .append("\n\n");
            
            AnswerResponse.Citation citation = AnswerResponse.Citation.builder()
                    .episodeId(episode.getId())
                    .episodeTitle(episode.getTitle())
                    .audioUrl(episode.getAudioUrl())
                    .startMs(chunk.getStartMs())
                    .endMs(chunk.getEndMs())
                    .speakerLabels(chunk.getSpeakerLabels())
                    .textSnippet(chunk.getText().substring(0, Math.min(200, chunk.getText().length())))
                    .listenLink("/episodes/" + episode.getId() + "?t=" + (chunk.getStartMs() / 1000))
                    .build();
            
            citations.add(citation);
        }
        
        // Generate answer using LLM
        String answer = ollamaService.generateAnswer(question, contextBuilder.toString());
        
        return AnswerResponse.builder()
                .answer(answer)
                .citations(citations)
                .build();
    }
    
    private String formatTimestamp(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60);
        } else {
            return String.format("%d:%02d", minutes, seconds % 60);
        }
    }
}
