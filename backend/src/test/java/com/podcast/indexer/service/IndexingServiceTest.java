package com.podcast.indexer.service;

import com.podcast.indexer.model.Episode;
import com.podcast.indexer.model.Podcast;
import com.podcast.indexer.model.TranscriptSegment;
import com.podcast.indexer.model.EmbeddingChunk;
import com.podcast.indexer.model.ProcessingStatus;
import com.podcast.indexer.repository.EmbeddingChunkRepository;
import com.podcast.indexer.repository.EpisodeRepository;
import com.podcast.indexer.repository.TranscriptSegmentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class IndexingServiceTest {

    @Test
    void indexEpisodeStoresSpeakerLabelsInChunks() {
        EpisodeRepository episodeRepository = mock(EpisodeRepository.class);
        TranscriptSegmentRepository transcriptSegmentRepository = mock(TranscriptSegmentRepository.class);
        EmbeddingChunkRepository embeddingChunkRepository = mock(EmbeddingChunkRepository.class);
        OllamaService ollamaService = mock(OllamaService.class);

        Podcast podcast = Podcast.builder().id(1L).title("Test Podcast").build();
        Episode episode = Episode.builder()
                .id(10L)
                .podcast(podcast)
                .title("Episode 1")
                .audioUrl("https://example.com/audio.mp3")
                .status(ProcessingStatus.TRANSCRIBED)
                .createdAt(LocalDateTime.now())
                .build();

        TranscriptSegment segmentOne = TranscriptSegment.builder()
                .episode(episode)
                .partIndex(0)
                .segmentIndex(0)
                .startMs(0L)
                .endMs(1000L)
                .text("Hello there")
                .speakerLabel("Speaker 1")
                .build();

        TranscriptSegment segmentTwo = TranscriptSegment.builder()
                .episode(episode)
                .partIndex(0)
                .segmentIndex(1)
                .startMs(1000L)
                .endMs(2000L)
                .text("General Kenobi")
                .speakerLabel("Speaker 2")
                .build();

        when(episodeRepository.findById(episode.getId())).thenReturn(Optional.of(episode));
        when(embeddingChunkRepository.existsByEpisodeId(episode.getId())).thenReturn(false);
        when(transcriptSegmentRepository.findByEpisodeIdOrderByPartIndexAscSegmentIndexAsc(episode.getId()))
                .thenReturn(List.of(segmentOne, segmentTwo));
        when(ollamaService.generateEmbedding(anyString())).thenReturn(List.of(0.1, 0.2, 0.3));

        IndexingService service = new IndexingService(
                episodeRepository,
                transcriptSegmentRepository,
                embeddingChunkRepository,
                ollamaService
        );

        service.indexEpisode(episode.getId());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EmbeddingChunk>> chunkCaptor = ArgumentCaptor.forClass(List.class);
        verify(embeddingChunkRepository).saveAll(chunkCaptor.capture());
        EmbeddingChunk chunk = chunkCaptor.getValue().get(0);

        assertThat(chunk.getSpeakerLabels()).isEqualTo("Speaker 1, Speaker 2");
        assertThat(chunk.getText()).contains("Speaker 1: Hello there", "Speaker 2: General Kenobi");
        assertThat(episode.getStatus()).isEqualTo(ProcessingStatus.INDEXED);
    }
}
