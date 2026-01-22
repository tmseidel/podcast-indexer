package com.podcast.indexer.repository;

import com.podcast.indexer.model.EmbeddingChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmbeddingChunkRepository extends JpaRepository<EmbeddingChunk, Long> {
    
    @Query(value = "SELECT c.* FROM embedding_chunks c " +
            "INNER JOIN episodes e ON c.episode_id = e.id " +
            "WHERE e.podcast_id = :podcastId " +
            "ORDER BY c.embedding <-> CAST(:queryEmbedding AS vector) " +
            "LIMIT :topK", 
            nativeQuery = true)
    List<EmbeddingChunk> findTopKSimilarByPodcast(
        @Param("podcastId") Long podcastId,
        @Param("queryEmbedding") String queryEmbedding,
        @Param("topK") int topK
    );
    
    boolean existsByEpisodeId(Long episodeId);
    
    List<EmbeddingChunk> findByEpisodeId(Long episodeId);
}
