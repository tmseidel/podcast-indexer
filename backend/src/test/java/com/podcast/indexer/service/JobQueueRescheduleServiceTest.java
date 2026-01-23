package com.podcast.indexer.service;

import com.podcast.indexer.model.Episode;
import com.podcast.indexer.model.Podcast;
import com.podcast.indexer.model.ProcessingStatus;
import com.podcast.indexer.repository.EpisodeRepository;
import com.podcast.indexer.repository.EmbeddingChunkRepository;
import com.podcast.indexer.repository.TranscriptSegmentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JobQueueRescheduleServiceTest {

    @Test
    void rescheduleMissingIndexesQueuesEpisodesWithTranscripts() {
        EpisodeRepository episodeRepository = mock(EpisodeRepository.class);
        TranscriptSegmentRepository transcriptSegmentRepository = mock(TranscriptSegmentRepository.class);
        EmbeddingChunkRepository embeddingChunkRepository = mock(EmbeddingChunkRepository.class);
        JobQueueService jobQueueService = mock(JobQueueService.class);

        Podcast podcast = Podcast.builder().id(1L).title("Test").build();
        Episode episode = Episode.builder()
                .id(22L)
                .podcast(podcast)
                .status(ProcessingStatus.TRANSCRIBED)
                .createdAt(LocalDateTime.now())
                .build();

        when(episodeRepository.findByStatus(ProcessingStatus.TRANSCRIBED))
                .thenReturn(List.of(episode));
        when(transcriptSegmentRepository.existsByEpisodeId(episode.getId())).thenReturn(true);
        when(embeddingChunkRepository.existsByEpisodeId(episode.getId())).thenReturn(false);

        JobQueueRescheduleService service = new JobQueueRescheduleService(
                episodeRepository,
                transcriptSegmentRepository,
                embeddingChunkRepository,
                jobQueueService
        );

        service.rescheduleMissingIndexes();

        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        verify(jobQueueService).queueIndexEpisodeJob(idCaptor.capture());
        assertThat(idCaptor.getValue()).isEqualTo(episode.getId());
        verify(episodeRepository, never()).save(any());
        assertThat(episode.getStatus()).isEqualTo(ProcessingStatus.TRANSCRIBED);
    }

    @Test
    void rescheduleMissingIndexesSkipsEpisodesWithoutTranscripts() {
        EpisodeRepository episodeRepository = mock(EpisodeRepository.class);
        TranscriptSegmentRepository transcriptSegmentRepository = mock(TranscriptSegmentRepository.class);
        EmbeddingChunkRepository embeddingChunkRepository = mock(EmbeddingChunkRepository.class);
        JobQueueService jobQueueService = mock(JobQueueService.class);

        Podcast podcast = Podcast.builder().id(1L).title("Test").build();
        Episode episode = Episode.builder()
                .id(23L)
                .podcast(podcast)
                .status(ProcessingStatus.TRANSCRIBED)
                .createdAt(LocalDateTime.now())
                .build();

        when(episodeRepository.findByStatus(ProcessingStatus.TRANSCRIBED))
                .thenReturn(Collections.singletonList(episode));
        when(transcriptSegmentRepository.existsByEpisodeId(episode.getId())).thenReturn(false);

        JobQueueRescheduleService service = new JobQueueRescheduleService(
                episodeRepository,
                transcriptSegmentRepository,
                embeddingChunkRepository,
                jobQueueService
        );

        service.rescheduleMissingIndexes();

        verifyNoInteractions(embeddingChunkRepository);
        verifyNoInteractions(jobQueueService);
        verify(episodeRepository, never()).save(any());
    }

    @Test
    void rescheduleStuckTranscriptionsQueuesDownloadWhenAudioMissing() {
        EpisodeRepository episodeRepository = mock(EpisodeRepository.class);
        TranscriptSegmentRepository transcriptSegmentRepository = mock(TranscriptSegmentRepository.class);
        EmbeddingChunkRepository embeddingChunkRepository = mock(EmbeddingChunkRepository.class);
        JobQueueService jobQueueService = mock(JobQueueService.class);

        Podcast podcast = Podcast.builder().id(1L).title("Test").build();
        Episode episode = Episode.builder()
                .id(24L)
                .podcast(podcast)
                .status(ProcessingStatus.TRANSCRIBING)
                .createdAt(LocalDateTime.now())
                .audioFilePath(null)
                .build();

        when(episodeRepository.findByStatus(ProcessingStatus.TRANSCRIBING))
                .thenReturn(Collections.singletonList(episode));
        when(transcriptSegmentRepository.existsByEpisodeId(episode.getId())).thenReturn(false);

        JobQueueRescheduleService service = new JobQueueRescheduleService(
                episodeRepository,
                transcriptSegmentRepository,
                embeddingChunkRepository,
                jobQueueService
        );

        service.rescheduleStuckTranscriptions();

        verify(jobQueueService).queueDownloadAudioJob(episode.getId());
        verify(episodeRepository).save(episode);
        assertThat(episode.getStatus()).isEqualTo(ProcessingStatus.DISCOVERED);
    }

    @Test
    void rescheduleStuckTranscriptionsQueuesTranscribeWhenAudioPresent() {
        EpisodeRepository episodeRepository = mock(EpisodeRepository.class);
        TranscriptSegmentRepository transcriptSegmentRepository = mock(TranscriptSegmentRepository.class);
        EmbeddingChunkRepository embeddingChunkRepository = mock(EmbeddingChunkRepository.class);
        JobQueueService jobQueueService = mock(JobQueueService.class);

        Podcast podcast = Podcast.builder().id(1L).title("Test").build();
        Episode episode = Episode.builder()
                .id(25L)
                .podcast(podcast)
                .status(ProcessingStatus.TRANSCRIBING)
                .createdAt(LocalDateTime.now())
                .audioFilePath("/tmp/audio.mp3")
                .build();

        when(episodeRepository.findByStatus(ProcessingStatus.TRANSCRIBING))
                .thenReturn(Collections.singletonList(episode));
        when(transcriptSegmentRepository.existsByEpisodeId(episode.getId())).thenReturn(false);

        JobQueueRescheduleService service = new JobQueueRescheduleService(
                episodeRepository,
                transcriptSegmentRepository,
                embeddingChunkRepository,
                jobQueueService
        );

        service.rescheduleStuckTranscriptions();

        verify(jobQueueService).queueTranscribeJob(episode.getId(), 0, episode.getAudioFilePath());
        verify(episodeRepository).save(episode);
        assertThat(episode.getStatus()).isEqualTo(ProcessingStatus.DOWNLOADED);
    }
}
