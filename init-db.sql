-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Add columns for speaker diarization metadata
ALTER TABLE transcript_segments
    ADD COLUMN IF NOT EXISTS speaker_label TEXT;

ALTER TABLE embedding_chunks
    ADD COLUMN IF NOT EXISTS speaker_labels TEXT;

-- Grant necessary permissions
GRANT ALL PRIVILEGES ON DATABASE podcast_indexer TO postgres;
