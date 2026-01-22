package com.podcast.indexer.model;

public enum ProcessingStatus {
    DISCOVERED,        // Episode found in feed
    DOWNLOADING,       // Audio being downloaded
    DOWNLOADED,        // Audio downloaded
    TRANSCRIBING,      // Being transcribed
    TRANSCRIBED,       // Transcription complete
    INDEXING,          // Generating embeddings
    INDEXED,           // Fully processed
    FAILED             // Processing failed
}
