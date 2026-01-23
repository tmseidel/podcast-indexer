package com.podcast.indexer.service;

import com.podcast.indexer.model.Episode;
import com.podcast.indexer.model.ProcessingStatus;
import com.podcast.indexer.repository.EmbeddingChunkRepository;
import com.podcast.indexer.repository.EpisodeRepository;
import com.podcast.indexer.repository.TranscriptSegmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobQueueRescheduleService {

    private final EpisodeRepository episodeRepository;
    private final TranscriptSegmentRepository transcriptSegmentRepository;
    private final EmbeddingChunkRepository embeddingChunkRepository;
    private final JobQueueService jobQueueService;

    @Scheduled(fixedDelay = 60000)
    public void rescheduleStuckTranscriptions() {
        List<Episode> episodes = episodeRepository.findByStatus(ProcessingStatus.TRANSCRIBING);
        for (Episode episode : episodes) {
            if (transcriptSegmentRepository.existsByEpisodeId(episode.getId())) {
                if (episode.getStatus() != ProcessingStatus.TRANSCRIBED) {
                    episode.setStatus(ProcessingStatus.TRANSCRIBED);
                    episodeRepository.save(episode);
                }
                if (!embeddingChunkRepository.existsByEpisodeId(episode.getId())) {
                    log.info("Rescheduling indexing for episode {}", episode.getId());
                    jobQueueService.queueIndexEpisodeJob(episode.getId());
                }
                continue;
            }

            if (episode.getAudioFilePath() == null || episode.getAudioFilePath().isBlank()) {
                log.warn("Episode {} stuck in TRANSCRIBING without audio, re-queueing download", episode.getId());
                episode.setStatus(ProcessingStatus.DISCOVERED);
                episodeRepository.save(episode);
                jobQueueService.queueDownloadAudioJob(episode.getId());
            } else {
                log.warn("Episode {} stuck in TRANSCRIBING, re-queueing transcription", episode.getId());
                episode.setStatus(ProcessingStatus.DOWNLOADED);
                episodeRepository.save(episode);
                jobQueueService.queueTranscribeJob(episode.getId(), 0, episode.getAudioFilePath());
            }
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void rescheduleMissingIndexes() {
        List<Episode> episodes = episodeRepository.findByStatus(ProcessingStatus.TRANSCRIBED);
        for (Episode episode : episodes) {
            if (!transcriptSegmentRepository.existsByEpisodeId(episode.getId())) {
                continue;
            }
            if (embeddingChunkRepository.existsByEpisodeId(episode.getId())) {
                continue;
            }
            log.info("Rescheduling indexing for episode {}", episode.getId());
            jobQueueService.queueIndexEpisodeJob(episode.getId());
        }
    }
}
