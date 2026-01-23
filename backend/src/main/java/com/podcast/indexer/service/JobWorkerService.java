package com.podcast.indexer.service;

import com.podcast.indexer.config.PodcastConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobWorkerService {
    
    private final JobQueueService jobQueueService;
    private final RssFeedService rssFeedService;
    private final AudioService audioService;
    private final WhisperService whisperService;
    private final IndexingService indexingService;
    private final PodcastConfig config;

    private ExecutorService executorService;
    private final AtomicInteger activeJobs = new AtomicInteger(0);
    private final AtomicReference<List<ActiveJob>> activeJobSnapshots = new AtomicReference<>(List.of());
    private final Object activeJobLock = new Object();
    
    @PostConstruct
    public void startWorkers() {
        int parallelism = Math.max(1, config.getJobs().getWorker().getParallelism());
        executorService = Executors.newFixedThreadPool(parallelism);
        for (int i = 0; i < parallelism; i++) {
            executorService.submit(this::runWorkerLoop);
        }
    }

    @PreDestroy
    public void stopWorkers() {
        if (executorService != null) {
            executorService.shutdownNow();
            try {
                executorService.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public JobWorkerStatus getStatus(int queuePreviewLimit) {
        JobQueueService.QueueSnapshot snapshot = jobQueueService.getQueueSnapshot(queuePreviewLimit);
        return JobWorkerStatus.builder()
                .parallelism(config.getJobs().getWorker().getParallelism())
                .activeJobCount(activeJobs.get())
                .activeJobs(activeJobSnapshots.get())
                .queueSize(snapshot.getTotalSize())
                .queuedJobs(snapshot.getJobs())
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    private void runWorkerLoop() {
        long pollDelay = config.getJobs().getWorker().getPollDelayMs();
        long dequeueTimeoutSeconds = config.getJobs().getWorker().getDequeueTimeoutSeconds();
        while (!Thread.currentThread().isInterrupted()) {
            JobQueueService.Job job = jobQueueService.dequeueJob(dequeueTimeoutSeconds);
            if (job == null) {
                sleep(pollDelay);
                continue;
            }
            activeJobs.incrementAndGet();
            updateActiveJobs(job, true);
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
            } finally {
                updateActiveJobs(job, false);
                activeJobs.decrementAndGet();
            }
        }
    }

    private void updateActiveJobs(JobQueueService.Job job, boolean add) {
        synchronized (activeJobLock) {
            List<ActiveJob> current = new ArrayList<>(activeJobSnapshots.get());
            if (add) {
                current.add(ActiveJob.from(job));
            } else {
                current.removeIf(activeJob -> activeJob.getJobId().equals(job.getJobId()));
            }
            activeJobSnapshots.set(List.copyOf(current));
        }
    }

    private void sleep(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class JobWorkerStatus {
        private int parallelism;
        private int activeJobCount;
        private long queueSize;
        private List<JobQueueService.Job> queuedJobs;
        private List<ActiveJob> activeJobs;
        private LocalDateTime lastUpdated;
    }

    @lombok.Data
    @lombok.Builder
    public static class ActiveJob {
        private String jobId;
        private JobQueueService.JobType type;
        private Long resourceId;
        private Integer partIndex;
        private String audioFilePath;
        private LocalDateTime startedAt;

        public static ActiveJob from(JobQueueService.Job job) {
            return ActiveJob.builder()
                    .jobId(job.getJobId())
                    .type(job.getType())
                    .resourceId(job.getResourceId())
                    .partIndex(job.getPartIndex())
                    .audioFilePath(job.getAudioFilePath())
                    .startedAt(LocalDateTime.now())
                    .build();
        }
    }
}
