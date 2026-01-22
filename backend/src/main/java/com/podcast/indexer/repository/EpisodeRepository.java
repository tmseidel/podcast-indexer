package com.podcast.indexer.repository;

import com.podcast.indexer.model.Episode;
import com.podcast.indexer.model.ProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EpisodeRepository extends JpaRepository<Episode, Long> {
    Optional<Episode> findByGuid(String guid);
    Optional<Episode> findByContentHash(String contentHash);
    boolean existsByGuid(String guid);
    boolean existsByContentHash(String contentHash);
    List<Episode> findByPodcastId(Long podcastId);
    List<Episode> findByStatus(ProcessingStatus status);
    List<Episode> findByPodcastIdAndStatus(Long podcastId, ProcessingStatus status);
}
