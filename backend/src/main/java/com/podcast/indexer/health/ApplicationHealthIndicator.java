package com.podcast.indexer.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class ApplicationHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        return Health.up()
                .withDetail("application", "Podcast Indexer")
                .withDetail("status", "Running")
                .build();
    }
}
