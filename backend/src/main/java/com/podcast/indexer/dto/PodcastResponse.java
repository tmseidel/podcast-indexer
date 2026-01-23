package com.podcast.indexer.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PodcastResponse {
    private Long id;
    private String feedUrl;
    private String title;
    private String description;
    private String imageUrl;
    private String author;
    private LocalDateTime createdAt;
    private LocalDateTime lastSyncedAt;
    private LocalDate downloadUntilDate;
    private List<EpisodeResponse> episodes;
}
