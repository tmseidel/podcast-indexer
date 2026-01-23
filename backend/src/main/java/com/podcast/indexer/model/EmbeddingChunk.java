package com.podcast.indexer.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.ColumnTransformer;

import java.time.LocalDateTime;

@Entity
@Table(name = "embedding_chunks", indexes = {
    @Index(name = "idx_chunk_episode", columnList = "episode_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmbeddingChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id", nullable = false)
    private Episode episode;
    
    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;
    
    @Column(name = "start_ms", nullable = false)
    private Long startMs;
    
    @Column(name = "end_ms", nullable = false)
    private Long endMs;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    @Column(name = "speaker_labels", columnDefinition = "TEXT")
    private String speakerLabels;
    
    @Column(columnDefinition = "vector(768)")
    @ColumnTransformer(write = "?::vector")
    private String embedding;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
