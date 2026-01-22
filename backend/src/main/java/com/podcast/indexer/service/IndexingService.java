package com.podcast.indexer.service;

import com.podcast.indexer.model.EmbeddingChunk;
import com.podcast.indexer.model.Episode;
import com.podcast.indexer.model.ProcessingStatus;
import com.podcast.indexer.model.TranscriptSegment;
import com.podcast.indexer.repository.EmbeddingChunkRepository;
import com.podcast.indexer.repository.EpisodeRepository;
import com.podcast.indexer.repository.TranscriptSegmentRepository;
import com.podcast.indexer.util.EmbeddingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingService {
    
    private final EpisodeRepository episodeRepository;
    private final TranscriptSegmentRepository transcriptSegmentRepository;
    private final EmbeddingChunkRepository embeddingChunkRepository;
    private final OllamaService ollamaService;
    
    private static final int CHUNK_SIZE_SEGMENTS = 10; // Group ~10 segments per chunk
    
    @Transactional
    public void indexEpisode(Long episodeId) {
        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new IllegalArgumentException("Episode not found"));
        
        // Check if already indexed
        if (embeddingChunkRepository.existsByEpisodeId(episodeId)) {
            log.info("Episode {} already indexed, skipping", episodeId);
            return;
        }
        
        episode.setStatus(ProcessingStatus.INDEXING);
        episodeRepository.save(episode);
        
        try {
            List<TranscriptSegment> segments = 
                    transcriptSegmentRepository.findByEpisodeIdOrderByPartIndexAscSegmentIndexAsc(episodeId);
            
            if (segments.isEmpty()) {
                log.warn("No transcript segments found for episode {}", episodeId);
                return;
            }
            
            List<EmbeddingChunk> chunks = new ArrayList<>();
            int chunkIndex = 0;
            
            for (int i = 0; i < segments.size(); i += CHUNK_SIZE_SEGMENTS) {
                int endIdx = Math.min(i + CHUNK_SIZE_SEGMENTS, segments.size());
                List<TranscriptSegment> chunkSegments = segments.subList(i, endIdx);
                
                // Combine text
                StringBuilder textBuilder = new StringBuilder();
                long startMs = chunkSegments.get(0).getStartMs();
                long endMs = chunkSegments.get(chunkSegments.size() - 1).getEndMs();
                
                for (TranscriptSegment seg : chunkSegments) {
                    textBuilder.append(seg.getText()).append(" ");
                }
                
                String text = textBuilder.toString().trim();
                
                // Generate embedding
                List<Double> embedding = ollamaService.generateEmbedding(text);
                String embeddingStr = EmbeddingUtils.convertEmbeddingToString(embedding);
                
                EmbeddingChunk chunk = EmbeddingChunk.builder()
                        .episode(episode)
                        .chunkIndex(chunkIndex)
                        .startMs(startMs)
                        .endMs(endMs)
                        .text(text)
                        .embedding(embeddingStr)
                        .build();
                
                chunks.add(chunk);
                chunkIndex++;
            }
            
            embeddingChunkRepository.saveAll(chunks);
            log.info("Indexed {} chunks for episode {}: {}", chunks.size(), episodeId, episode.getTitle());
            
            episode.setStatus(ProcessingStatus.INDEXED);
            episodeRepository.save(episode);
        } catch (Exception e) {
            log.error("Failed to index episode {}", episodeId, e);
            episode.setStatus(ProcessingStatus.FAILED);
            episodeRepository.save(episode);
        }
    }
}
