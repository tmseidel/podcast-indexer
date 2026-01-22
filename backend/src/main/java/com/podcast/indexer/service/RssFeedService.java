package com.podcast.indexer.service;

import com.podcast.indexer.model.Episode;
import com.podcast.indexer.model.Podcast;
import com.podcast.indexer.model.ProcessingStatus;
import com.podcast.indexer.repository.EpisodeRepository;
import com.podcast.indexer.repository.PodcastRepository;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RssFeedService {
    
    private final PodcastRepository podcastRepository;
    private final EpisodeRepository episodeRepository;
    private final JobQueueService jobQueueService;
    
    @Transactional
    public Podcast addPodcast(String feedUrl) {
        if (podcastRepository.existsByFeedUrl(feedUrl)) {
            throw new IllegalArgumentException("Podcast already exists");
        }
        
        SyndFeed feed = fetchFeed(feedUrl);
        
        Podcast podcast = Podcast.builder()
                .feedUrl(feedUrl)
                .title(feed.getTitle())
                .description(feed.getDescription())
                .author(feed.getAuthor())
                .imageUrl(feed.getImage() != null ? feed.getImage().getUrl() : null)
                .build();
        
        podcast = podcastRepository.save(podcast);
        log.info("Added podcast: {} (ID: {})", podcast.getTitle(), podcast.getId());
        
        // Queue sync job
        jobQueueService.queueSyncEpisodesJob(podcast.getId());
        
        return podcast;
    }
    
    @Transactional
    public void syncEpisodes(Long podcastId) {
        Podcast podcast = podcastRepository.findById(podcastId)
                .orElseThrow(() -> new IllegalArgumentException("Podcast not found"));
        
        SyndFeed feed = fetchFeed(podcast.getFeedUrl());
        List<Episode> newEpisodes = new ArrayList<>();
        
        for (SyndEntry entry : feed.getEntries()) {
            String guid = entry.getUri();
            String audioUrl = extractAudioUrl(entry);
            
            if (audioUrl == null) {
                log.warn("No audio URL found for entry: {}", entry.getTitle());
                continue;
            }
            
            // Check if episode already exists
            boolean exists = false;
            if (guid != null && !guid.isEmpty()) {
                exists = episodeRepository.existsByGuid(guid);
            } else {
                // Use content hash as fallback
                String contentHash = generateContentHash(entry.getTitle(), audioUrl);
                exists = episodeRepository.existsByContentHash(contentHash);
                guid = null;
            }
            
            if (!exists) {
                Episode episode = Episode.builder()
                        .podcast(podcast)
                        .title(entry.getTitle())
                        .description(entry.getDescription() != null ? entry.getDescription().getValue() : null)
                        .guid(guid)
                        .contentHash(guid == null ? generateContentHash(entry.getTitle(), audioUrl) : null)
                        .audioUrl(audioUrl)
                        .publishedDate(entry.getPublishedDate() != null ? 
                                LocalDateTime.ofInstant(entry.getPublishedDate().toInstant(), ZoneId.systemDefault()) : null)
                        .status(ProcessingStatus.DISCOVERED)
                        .build();
                
                newEpisodes.add(episode);
            }
        }
        
        if (!newEpisodes.isEmpty()) {
            episodeRepository.saveAll(newEpisodes);
            log.info("Discovered {} new episodes for podcast: {}", newEpisodes.size(), podcast.getTitle());
            
            // Queue download jobs for new episodes
            for (Episode episode : newEpisodes) {
                jobQueueService.queueDownloadAudioJob(episode.getId());
            }
        }
        
        podcast.setLastSyncedAt(LocalDateTime.now());
        podcastRepository.save(podcast);
    }
    
    private SyndFeed fetchFeed(String feedUrl) {
        try {
            URL url = new URL(feedUrl);
            SyndFeedInput input = new SyndFeedInput();
            return input.build(new XmlReader(url));
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch RSS feed: " + feedUrl, e);
        }
    }
    
    private String extractAudioUrl(SyndEntry entry) {
        if (entry.getEnclosures() != null && !entry.getEnclosures().isEmpty()) {
            return entry.getEnclosures().get(0).getUrl();
        }
        return null;
    }
    
    private String generateContentHash(String title, String audioUrl) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String content = title + "|" + audioUrl;
            byte[] hash = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate content hash", e);
        }
    }
}
