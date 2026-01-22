package com.podcast.indexer.repository;

import com.podcast.indexer.model.TranscriptSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TranscriptSegmentRepository extends JpaRepository<TranscriptSegment, Long> {
    List<TranscriptSegment> findByEpisodeIdOrderByPartIndexAscSegmentIndexAsc(Long episodeId);
    boolean existsByEpisodeId(Long episodeId);
}
