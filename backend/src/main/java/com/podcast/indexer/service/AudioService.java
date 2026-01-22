package com.podcast.indexer.service;

import com.podcast.indexer.config.PodcastConfig;
import com.podcast.indexer.model.Episode;
import com.podcast.indexer.model.ProcessingStatus;
import com.podcast.indexer.repository.EpisodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AudioService {
    
    private final EpisodeRepository episodeRepository;
    private final PodcastConfig config;
    private final JobQueueService jobQueueService;
    
    @Transactional
    public void downloadAudio(Long episodeId) {
        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new IllegalArgumentException("Episode not found"));
        
        if (episode.getStatus() != ProcessingStatus.DISCOVERED) {
            log.info("Episode {} already processed, skipping download", episodeId);
            return;
        }
        
        episode.setStatus(ProcessingStatus.DOWNLOADING);
        episodeRepository.save(episode);
        
        try {
            String audioPath = downloadFile(episode.getAudioUrl(), episodeId);
            episode.setAudioFilePath(audioPath);
            
            // Get duration
            Integer durationSeconds = getAudioDuration(audioPath);
            episode.setDurationSeconds(durationSeconds);
            
            episode.setStatus(ProcessingStatus.DOWNLOADED);
            episodeRepository.save(episode);
            
            log.info("Downloaded audio for episode {}: {} ({} seconds)", 
                    episodeId, episode.getTitle(), durationSeconds);
            
            // Check if splitting is needed
            int maxMinutes = config.getAudio().getMaxMinutesBeforeSplit();
            if (durationSeconds != null && durationSeconds > maxMinutes * 60) {
                List<String> parts = splitAudio(audioPath, maxMinutes * 60);
                log.info("Split episode {} into {} parts", episodeId, parts.size());
                
                // Queue transcription for each part
                for (int i = 0; i < parts.size(); i++) {
                    jobQueueService.queueTranscribeJob(episodeId, i, parts.get(i));
                }
            } else {
                // Queue transcription for whole file
                jobQueueService.queueTranscribeJob(episodeId, 0, audioPath);
            }
        } catch (Exception e) {
            log.error("Failed to download audio for episode {}", episodeId, e);
            episode.setStatus(ProcessingStatus.FAILED);
            episodeRepository.save(episode);
        }
    }
    
    private String downloadFile(String audioUrl, Long episodeId) throws IOException {
        Path audioDir = Paths.get(config.getAudio().getStorage().getPath());
        Files.createDirectories(audioDir);
        
        String filename = "episode_" + episodeId + getFileExtension(audioUrl);
        Path targetPath = audioDir.resolve(filename);
        
        URL url = new URL(audioUrl);
        try (InputStream in = url.openStream()) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
        
        return targetPath.toString();
    }
    
    private String getFileExtension(String url) {
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.contains(".mp3")) return ".mp3";
        if (lowerUrl.contains(".m4a")) return ".m4a";
        if (lowerUrl.contains(".wav")) return ".wav";
        if (lowerUrl.contains(".ogg")) return ".ogg";
        return ".mp3"; // default
    }
    
    private Integer getAudioDuration(String audioPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffprobe", "-v", "error", 
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    audioPath
            );
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String output = reader.readLine();
            process.waitFor();
            
            if (output != null && !output.isEmpty()) {
                return (int) Double.parseDouble(output);
            }
        } catch (Exception e) {
            log.warn("Failed to get audio duration for {}", audioPath, e);
        }
        return null;
    }
    
    private List<String> splitAudio(String audioPath, int segmentDurationSeconds) throws IOException, InterruptedException {
        List<String> parts = new ArrayList<>();
        Path inputPath = Paths.get(audioPath);
        String baseName = inputPath.getFileName().toString().replaceFirst("[.][^.]+$", "");
        Path parentDir = inputPath.getParent();
        
        Integer totalDuration = getAudioDuration(audioPath);
        if (totalDuration == null) {
            throw new IOException("Could not determine audio duration");
        }
        
        int partIndex = 0;
        for (int startTime = 0; startTime < totalDuration; startTime += segmentDurationSeconds) {
            String outputPath = parentDir.resolve(baseName + "_part" + partIndex + getFileExtension(audioPath)).toString();
            
            // Note: Using -c copy for fast splitting. This may cause slight inaccuracies
            // at split boundaries (especially with VBR files). For precise splits, 
            // use re-encoding: "-c:a aac -b:a 128k" instead of "-c copy"
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-i", audioPath,
                    "-ss", String.valueOf(startTime),
                    "-t", String.valueOf(segmentDurationSeconds),
                    "-c", "copy",
                    "-y",
                    outputPath
            );
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                throw new IOException("ffmpeg failed with exit code " + exitCode);
            }
            
            parts.add(outputPath);
            partIndex++;
        }
        
        return parts;
    }
}
