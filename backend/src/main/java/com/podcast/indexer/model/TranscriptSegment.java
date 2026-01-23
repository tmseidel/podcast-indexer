package com.podcast.indexer.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "transcript_segments", indexes = {
    @Index(name = "idx_segment_episode", columnList = "episode_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranscriptSegment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id", nullable = false)
    private Episode episode;
    
    @Column(name = "part_index", nullable = false)
    @Builder.Default
    private Integer partIndex = 0;
    
    @Column(name = "segment_index", nullable = false)
    private Integer segmentIndex;
    
    @Column(name = "start_ms", nullable = false)
    private Long startMs;
    
    @Column(name = "end_ms", nullable = false)
    private Long endMs;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    @Column(name = "speaker_label")
    private String speakerLabel;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
