package com.podcast.indexer.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddPodcastRequest {
    private String feedUrl;
    private LocalDate downloadUntilDate;
}
