package com.podcast.indexer.controller;

import com.podcast.indexer.dto.AddPodcastRequest;
import com.podcast.indexer.dto.EpisodeResponse;
import com.podcast.indexer.dto.PodcastResponse;
import com.podcast.indexer.model.Episode;
import com.podcast.indexer.model.Podcast;
import com.podcast.indexer.repository.EpisodeRepository;
import com.podcast.indexer.repository.PodcastRepository;
import com.podcast.indexer.service.RssFeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/podcasts")
@RequiredArgsConstructor
public class PodcastController {
    
    private final PodcastRepository podcastRepository;
    private final EpisodeRepository episodeRepository;
    private final RssFeedService rssFeedService;
    
    @GetMapping
    public List<PodcastResponse> getAllPodcasts() {
        return podcastRepository.findAll().stream()
                .map(this::toPodcastResponse)
                .collect(Collectors.toList());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PodcastResponse> getPodcast(@PathVariable Long id) {
        return podcastRepository.findById(id)
                .map(podcast -> ResponseEntity.ok(toPodcastResponseWithEpisodes(podcast)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<PodcastResponse> addPodcast(@RequestBody AddPodcastRequest request) {
        try {
            Podcast podcast = rssFeedService.addPodcast(request.getFeedUrl());
            return ResponseEntity.ok(toPodcastResponse(podcast));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/{id}/sync")
    public ResponseEntity<Void> syncPodcast(@PathVariable Long id) {
        if (!podcastRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        rssFeedService.syncEpisodes(id);
        return ResponseEntity.ok().build();
    }
    
    private PodcastResponse toPodcastResponse(Podcast podcast) {
        return PodcastResponse.builder()
                .id(podcast.getId())
                .feedUrl(podcast.getFeedUrl())
                .title(podcast.getTitle())
                .description(podcast.getDescription())
                .imageUrl(podcast.getImageUrl())
                .author(podcast.getAuthor())
                .createdAt(podcast.getCreatedAt())
                .lastSyncedAt(podcast.getLastSyncedAt())
                .build();
    }
    
    private PodcastResponse toPodcastResponseWithEpisodes(Podcast podcast) {
        List<Episode> episodes = episodeRepository.findByPodcastId(podcast.getId());
        PodcastResponse response = toPodcastResponse(podcast);
        response.setEpisodes(episodes.stream()
                .map(this::toEpisodeResponse)
                .collect(Collectors.toList()));
        return response;
    }
    
    private EpisodeResponse toEpisodeResponse(Episode episode) {
        return EpisodeResponse.builder()
                .id(episode.getId())
                .podcastId(episode.getPodcast().getId())
                .title(episode.getTitle())
                .description(episode.getDescription())
                .audioUrl(episode.getAudioUrl())
                .publishedDate(episode.getPublishedDate())
                .durationSeconds(episode.getDurationSeconds())
                .status(episode.getStatus())
                .createdAt(episode.getCreatedAt())
                .build();
    }
}
