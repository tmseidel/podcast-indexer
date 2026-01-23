package com.podcast.indexer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "podcast")
@Data
public class PodcastConfig {
    private Audio audio = new Audio();
    private Whisper whisper = new Whisper();
    private Ollama ollama = new Ollama();
    private Vector vector = new Vector();
    private Qa qa = new Qa();
    
    @Data
    public static class Audio {
        private Storage storage = new Storage();
        private int maxMinutesBeforeSplit = 60;
        
        @Data
        public static class Storage {
            private String path = "/app/data/audio";
        }
    }
    
    @Data
    public static class Whisper {
        private Service service = new Service();
        
        @Data
        public static class Service {
            private String url = "http://whisper-service:8000";
        }
    }
    
    @Data
    public static class Ollama {
        private Service service = new Service();
        private Embedding embedding = new Embedding();
        private Chat chat = new Chat();
        
        @Data
        public static class Service {
            private String url = "http://ollama:11434";
        }
        
        @Data
        public static class Embedding {
            private String model = "nomic-embed-text";
        }
        
        @Data
        public static class Chat {
            private String model = "llama2";
        }
    }
    
    @Data
    public static class Vector {
        private Search search = new Search();
        
        @Data
        public static class Search {
            private int topK = 5;
        }
    }

    @Data
    public static class Qa {
        private Cache cache = new Cache();

        @Data
        public static class Cache {
            // Limit TTL to 1 minute minimum to prevent immediate cache churn
            // and 24 hours maximum to keep answers reasonably fresh.
            private static final int MIN_TTL_MINUTES = 1;
            private static final int MAX_TTL_MINUTES = 1440;
            private int ttlMinutes = 60;

            public void setTtlMinutes(int ttlMinutes) {
                if (ttlMinutes < MIN_TTL_MINUTES || ttlMinutes > MAX_TTL_MINUTES) {
                    throw new IllegalArgumentException(
                            "QA cache TTL must be between " + MIN_TTL_MINUTES + " and " + MAX_TTL_MINUTES + " minutes");
                }
                this.ttlMinutes = ttlMinutes;
            }
        }
    }
}
