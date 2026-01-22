package com.podcast.indexer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobQueueService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String QUEUE_KEY = "podcast:jobs";
    
    public void queueSyncEpisodesJob(Long podcastId) {
        queueJob(new Job(JobType.SYNC_EPISODES, podcastId, null, null));
    }
    
    public void queueDownloadAudioJob(Long episodeId) {
        queueJob(new Job(JobType.DOWNLOAD_AUDIO, episodeId, null, null));
    }
    
    public void queueTranscribeJob(Long episodeId, int partIndex, String audioFilePath) {
        queueJob(new Job(JobType.TRANSCRIBE, episodeId, partIndex, audioFilePath));
    }
    
    public void queueIndexEpisodeJob(Long episodeId) {
        queueJob(new Job(JobType.INDEX_EPISODE, episodeId, null, null));
    }
    
    private void queueJob(Job job) {
        try {
            String jobJson = objectMapper.writeValueAsString(job);
            redisTemplate.opsForList().rightPush(QUEUE_KEY, jobJson);
            log.debug("Queued job: {}", job);
        } catch (Exception e) {
            log.error("Failed to queue job: {}", job, e);
        }
    }
    
    public Job dequeueJob(long timeoutSeconds) {
        try {
            String jobJson = redisTemplate.opsForList().leftPop(QUEUE_KEY, timeoutSeconds, TimeUnit.SECONDS);
            if (jobJson != null) {
                return objectMapper.readValue(jobJson, Job.class);
            }
        } catch (Exception e) {
            log.error("Failed to dequeue job", e);
        }
        return null;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Job {
        private JobType type;
        private Long resourceId; // podcast or episode ID
        private Integer partIndex;
        private String audioFilePath;
    }
    
    public enum JobType {
        SYNC_EPISODES,
        DOWNLOAD_AUDIO,
        TRANSCRIBE,
        INDEX_EPISODE
    }
}
