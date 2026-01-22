package com.podcast.indexer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobWorkerService {
    
    private final JobQueueService jobQueueService;
    private final RssFeedService rssFeedService;
    private final AudioService audioService;
    private final WhisperService whisperService;
    private final IndexingService indexingService;
    
    @Scheduled(fixedDelay = 1000) // Check every second
    public void processJobs() {
        JobQueueService.Job job = jobQueueService.dequeueJob(5);
        
        if (job != null) {
            try {
                log.info("Processing job: {}", job);
                
                switch (job.getType()) {
                    case SYNC_EPISODES:
                        rssFeedService.syncEpisodes(job.getResourceId());
                        break;
                    case DOWNLOAD_AUDIO:
                        audioService.downloadAudio(job.getResourceId());
                        break;
                    case TRANSCRIBE:
                        whisperService.transcribe(job.getResourceId(), job.getPartIndex(), job.getAudioFilePath());
                        break;
                    case INDEX_EPISODE:
                        indexingService.indexEpisode(job.getResourceId());
                        break;
                    default:
                        log.warn("Unknown job type: {}", job.getType());
                }
                
                log.info("Completed job: {}", job);
            } catch (Exception e) {
                log.error("Failed to process job: {}", job, e);
                // In production, you might want to implement retry logic or dead letter queue
            }
        }
    }
}
