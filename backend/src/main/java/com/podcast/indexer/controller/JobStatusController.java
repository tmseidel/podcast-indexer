package com.podcast.indexer.controller;

import com.podcast.indexer.config.PodcastConfig;
import com.podcast.indexer.service.JobWorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobStatusController {

    private final JobWorkerService jobWorkerService;
    private final PodcastConfig config;

    @GetMapping("/status")
    public JobWorkerService.JobWorkerStatus getStatus(
            @RequestParam(value = "limit", required = false) Integer limit) {
        int previewLimit = limit != null ? limit : config.getJobs().getQueue().getStatusLimit();
        return jobWorkerService.getStatus(previewLimit);
    }
}
