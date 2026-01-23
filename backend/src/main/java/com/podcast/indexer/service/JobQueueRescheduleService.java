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
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobQueueRescheduleService {

    private static final String RESCHEDULE_DELAY_MS = "${job.queue.reschedule.delay:60000}";

    private final EpisodeRepository episodeRepository;
    private final TranscriptSegmentRepository transcriptSegmentRepository;
    private final EmbeddingChunkRepository embeddingChunkRepository;
    private final JobQueueService jobQueueService;

    @Scheduled(fixedDelayString = RESCHEDULE_DELAY_MS)
    public void rescheduleStuckTranscriptions() {
        List<Episode> episodes = episodeRepository.findByStatus(ProcessingStatus.TRANSCRIBING);
        for (Episode episode : episodes) {
            if (transcriptSegmentRepository.existsByEpisodeId(episode.getId())) {
                episode.setStatus(ProcessingStatus.TRANSCRIBED);
                episodeRepository.save(episode);
                rescheduleIndexingIfReady(episode);
                continue;
            }

            if (!StringUtils.hasText(episode.getAudioFilePath())) {
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

    @Scheduled(fixedDelayString = RESCHEDULE_DELAY_MS)
    public void rescheduleMissingIndexes() {
        List<Episode> episodes = episodeRepository.findByStatus(ProcessingStatus.TRANSCRIBED);
        for (Episode episode : episodes) {
            rescheduleIndexingIfReady(episode);
        }
    }

    private void rescheduleIndexingIfReady(Episode episode) {
        if (!transcriptSegmentRepository.existsByEpisodeId(episode.getId())) {
            return;
        }
        if (embeddingChunkRepository.existsByEpisodeId(episode.getId())) {
            return;
        }
        log.info("Rescheduling indexing for episode {}", episode.getId());
        jobQueueService.queueIndexEpisodeJob(episode.getId());
    }
}
