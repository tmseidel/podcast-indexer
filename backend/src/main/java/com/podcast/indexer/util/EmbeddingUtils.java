package com.podcast.indexer.util;

import java.util.List;

public class EmbeddingUtils {
    
    private EmbeddingUtils() {
        // Utility class
    }
    
    /**
     * Converts a list of embedding doubles to a PostgreSQL vector string format.
     * Format: "[value1,value2,value3,...]"
     */
    public static String convertEmbeddingToString(List<Double> embedding) {
        if (embedding == null || embedding.isEmpty()) {
            throw new IllegalArgumentException("Embedding cannot be null or empty");
        }
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding.get(i));
        }
        sb.append("]");
        return sb.toString();
    }
}
