package com.podcast.indexer.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "episodes", indexes = {
    @Index(name = "idx_episode_guid", columnList = "guid"),
    @Index(name = "idx_episode_hash", columnList = "content_hash")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Episode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "podcast_id", nullable = false)
    private Podcast podcast;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(unique = true)
    private String guid;
    
    @Column(name = "content_hash", unique = true)
    private String contentHash;
    
    @Column(name = "audio_url", nullable = false)
    private String audioUrl;
    
    @Column(name = "audio_file_path")
    private String audioFilePath;
    
    @Column(name = "published_date")
    private LocalDateTime publishedDate;
    
    @Column(name = "duration_seconds")
    private Integer durationSeconds;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProcessingStatus status = ProcessingStatus.DISCOVERED;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "episode", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TranscriptSegment> transcriptSegments = new ArrayList<>();
    
    @OneToMany(mappedBy = "episode", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EmbeddingChunk> embeddingChunks = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
