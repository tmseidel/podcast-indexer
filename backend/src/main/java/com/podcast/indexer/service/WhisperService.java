package com.podcast.indexer.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.podcast.indexer.model.Episode;
import com.podcast.indexer.model.ProcessingStatus;
import com.podcast.indexer.model.TranscriptSegment;
import com.podcast.indexer.repository.EpisodeRepository;
import com.podcast.indexer.repository.TranscriptSegmentRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhisperService {
    
    @Qualifier("whisperWebClient")
    private final WebClient whisperWebClient;
    private final EpisodeRepository episodeRepository;
    private final TranscriptSegmentRepository transcriptSegmentRepository;
    private final JobQueueService jobQueueService;
    
    @Transactional
    public void transcribe(Long episodeId, int partIndex, String audioFilePath) {
        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new IllegalArgumentException("Episode not found"));
        
        // Check if already transcribed
        if (transcriptSegmentRepository.existsByEpisodeId(episodeId)) {
            log.info("Episode {} already has transcript, skipping", episodeId);
            return;
        }
        
        episode.setStatus(ProcessingStatus.TRANSCRIBING);
        episodeRepository.save(episode);
        
        try {
            WhisperResponse response = callWhisperService(audioFilePath);
            
            // Calculate time offset for this part
            long timeOffsetMs = 0;
            if (partIndex > 0) {
                // Get the max end time from previous parts
                List<TranscriptSegment> previousSegments = 
                        transcriptSegmentRepository.findByEpisodeIdOrderByPartIndexAscSegmentIndexAsc(episodeId);
                if (!previousSegments.isEmpty()) {
                    timeOffsetMs = previousSegments.stream()
                            .filter(s -> s.getPartIndex() < partIndex)
                            .mapToLong(TranscriptSegment::getEndMs)
                            .max()
                            .orElse(0);
                }
            }
            
            List<TranscriptSegment> segments = new ArrayList<>();
            for (int i = 0; i < response.getSegments().size(); i++) {
                WhisperSegment seg = response.getSegments().get(i);
                TranscriptSegment segment = TranscriptSegment.builder()
                        .episode(episode)
                        .partIndex(partIndex)
                        .segmentIndex(i)
                        .startMs((long) (seg.getStart() * 1000) + timeOffsetMs)
                        .endMs((long) (seg.getEnd() * 1000) + timeOffsetMs)
                        .text(seg.getText().trim())
                        .build();
                segments.add(segment);
            }
            
            transcriptSegmentRepository.saveAll(segments);
            log.info("Transcribed {} segments for episode {} part {}", 
                    segments.size(), episodeId, partIndex);
            
            episode.setStatus(ProcessingStatus.TRANSCRIBED);
            episodeRepository.save(episode);
            
            // Queue indexing job
            jobQueueService.queueIndexEpisodeJob(episodeId);
        } catch (Exception e) {
            log.error("Failed to transcribe episode {} part {}", episodeId, partIndex, e);
            episode.setStatus(ProcessingStatus.FAILED);
            episodeRepository.save(episode);
        }
    }
    
    private WhisperResponse callWhisperService(String audioFilePath) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new FileSystemResource(new File(audioFilePath)));
        builder.part("response_format", "verbose_json");
        
        return whisperWebClient.post()
                .uri("/transcribe")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(WhisperResponse.class)
                .block();
    }
    
    @Data
    public static class WhisperResponse {
        private String text;
        private List<WhisperSegment> segments;
    }
    
    @Data
    public static class WhisperSegment {
        private int id;
        private double start;
        private double end;
        private String text;
    }
}
