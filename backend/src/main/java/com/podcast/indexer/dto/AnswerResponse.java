package com.podcast.indexer.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerResponse {
    private String answer;
    private List<Citation> citations;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Citation {
        private Long episodeId;
        private String episodeTitle;
        private String audioUrl;
        private Long startMs;
        private Long endMs;
        private String speakerLabels;
        private String textSnippet;
        private String listenLink;
    }
}
