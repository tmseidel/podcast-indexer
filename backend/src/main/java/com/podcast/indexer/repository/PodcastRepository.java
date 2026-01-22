package com.podcast.indexer.repository;

import com.podcast.indexer.model.Podcast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PodcastRepository extends JpaRepository<Podcast, Long> {
    Optional<Podcast> findByFeedUrl(String feedUrl);
    boolean existsByFeedUrl(String feedUrl);
}
