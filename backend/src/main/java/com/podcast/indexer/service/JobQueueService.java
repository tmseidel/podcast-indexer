package com.podcast.indexer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobQueueService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String QUEUE_KEY = "podcast:jobs";
    
    public void queueSyncEpisodesJob(Long podcastId) {
        queueJob(new Job(JobType.SYNC_EPISODES, podcastId, null, null, null));
    }
    
    public void queueDownloadAudioJob(Long episodeId) {
        queueJob(new Job(JobType.DOWNLOAD_AUDIO, episodeId, null, null, null));
    }
    
    public void queueTranscribeJob(Long episodeId, int partIndex, String audioFilePath) {
        queueJob(new Job(JobType.TRANSCRIBE, episodeId, partIndex, audioFilePath, null));
    }
    
    public void queueIndexEpisodeJob(Long episodeId) {
        queueJob(new Job(JobType.INDEX_EPISODE, episodeId, null, null, null));
    }
    
    private void queueJob(Job job) {
        try {
            job.ensureJobId();
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
                Job job = objectMapper.readValue(jobJson, Job.class);
                job.ensureJobId();
                return job;
            }
        } catch (Exception e) {
            log.error("Failed to dequeue job", e);
        }
        return null;
    }

    public QueueSnapshot getQueueSnapshot(int limit) {
        List<String> items = redisTemplate.opsForList().range(QUEUE_KEY, 0, Math.max(0, limit - 1));
        List<Job> jobs = new ArrayList<>();
        if (items != null) {
            for (String item : items) {
                try {
                    Job job = objectMapper.readValue(item, Job.class);
                    job.ensureJobId();
                    jobs.add(job);
                } catch (Exception e) {
                    log.warn("Failed to parse queued job", e);
                }
            }
        }
        Long size = redisTemplate.opsForList().size(QUEUE_KEY);
        return new QueueSnapshot(size != null ? size : 0L, jobs);
    }

    @Data
    @AllArgsConstructor
    public static class QueueSnapshot {
        private long totalSize;
        private List<Job> jobs;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Job {
        private JobType type;
        private Long resourceId; // podcast or episode ID
        private Integer partIndex;
        private String audioFilePath;
        private String jobId;

        public void ensureJobId() {
            if (jobId == null || jobId.isBlank()) {
                jobId = UUID.randomUUID().toString();
            }
        }
    }
    
    public enum JobType {
        SYNC_EPISODES,
        DOWNLOAD_AUDIO,
        TRANSCRIBE,
        INDEX_EPISODE
    }
}
