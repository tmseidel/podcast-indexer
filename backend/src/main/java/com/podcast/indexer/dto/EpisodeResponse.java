package com.podcast.indexer.dto;

import com.podcast.indexer.model.ProcessingStatus;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EpisodeResponse {
    private Long id;
    private Long podcastId;
    private String title;
    private String description;
    private String audioUrl;
    private LocalDateTime publishedDate;
    private Integer durationSeconds;
    private ProcessingStatus status;
    private LocalDateTime createdAt;
}
