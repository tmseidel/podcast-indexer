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
    private Jobs jobs = new Jobs();
    
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
    public static class Jobs {
        private Worker worker = new Worker();
        private Queue queue = new Queue();

        @Data
        public static class Worker {
            private int parallelism = 1;
            private long pollDelayMs = 1000;
        }

        @Data
        public static class Queue {
            private int statusLimit = 50;
        }
    }
}
