# Podcast Indexer

A local-first podcast indexing and Q&A system that uses Whisper for transcription, pgvector for semantic search, and Ollama for embeddings and question answering.

## Features

- 🎙️ **Podcast Management**: Add podcasts via RSS feed URL
- 📥 **Automatic Episode Discovery**: Fetch and sync episodes from RSS feeds
- 🎵 **Audio Processing**: Download episodes with configurable splitting for long content
- 📝 **Transcription**: Local Whisper-based transcription with accurate timestamps
- 🔍 **Vector Search**: Semantic search using pgvector and Ollama embeddings
- 💬 **Q&A System**: Ask questions about podcast content with citations and timestamps
- 🔄 **Background Processing**: Redis-backed job queue for async processing
- 🐳 **Fully Dockerized**: Run everything locally with docker-compose

## Architecture

- **Backend**: Spring Boot (Java 17)
- **Frontend**: React
- **Database**: PostgreSQL with pgvector extension
- **Job Queue**: Redis
- **Transcription**: Local Whisper service (Python/Flask)
- **Embeddings & Chat**: Ollama (local LLM)
- **Audio Storage**: Docker volume

## Prerequisites

- Docker and Docker Compose
- At least 8GB RAM (16GB recommended for Ollama models)
- ~10GB disk space for models and audio files

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/tmseidel/podcast-indexer.git
cd podcast-indexer
```

### 2. Start the Stack (Automated)

Run the setup script to automatically start all services and pull required models:

```bash
./setup.sh
```

Or manually:

### 2. Start the Stack (Manual)

```bash
docker-compose up -d
```

This will start all services:
- PostgreSQL with pgvector (port 5432)
- Redis (port 6379)
- Whisper service (port 8000)
- Ollama (port 11434)
- Backend API (port 8080)
- Frontend (port 3000)

### 3. Pull Required Ollama Models

Pull the required models before starting services (or use the setup script which handles it for you):

```bash
# Ensure the ollama-data volume exists so model pulls persist
docker volume create podcast-indexer_ollama-data

# Pull embedding model
docker run --rm -v podcast-indexer_ollama-data:/root/.ollama ollama/ollama:latest ollama pull nomic-embed-text

# Pull chat model (you can use different models)
docker run --rm -v podcast-indexer_ollama-data:/root/.ollama ollama/ollama:latest ollama pull llama2
```

Alternative chat models you can use:
- `llama2` (default, ~4GB)
- `mistral` (~4GB)
- `llama3` (~4.7GB)
- `phi3` (~2.3GB, faster but less capable)

### 4. Access the Application

Open your browser and navigate to:
- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080

## Usage

### Adding a Podcast

1. Go to the main page at http://localhost:3000
2. Enter an RSS feed URL (e.g., `https://feeds.example.com/podcast.rss`)
3. (Optional) Pick a "Download episodes on or after" date to limit episode discovery
4. Click "Add Podcast"
5. The system will:
   - Fetch podcast metadata
   - Discover episodes
   - Start background processing automatically

### Processing Pipeline

Once a podcast is added, episodes go through these stages:

1. **DISCOVERED**: Episode found in RSS feed
2. **DOWNLOADING**: Audio file being downloaded
3. **DOWNLOADED**: Audio downloaded successfully
4. **TRANSCRIBING**: Being transcribed by Whisper
5. **TRANSCRIBED**: Transcription complete
6. **INDEXING**: Generating embeddings and storing in vector DB
7. **INDEXED**: Fully processed and searchable

You can monitor progress on the podcast detail page.

### Asking Questions

1. Navigate to the "Ask Question" page
2. Select a podcast from the dropdown
3. Type your question
4. Get an AI-generated answer with citations including:
   - Episode title
   - Timestamp ranges
   - Text snippets
   - Links to listen at specific timestamps

### Monitoring Job Queue

Visit the **Job Queue** page at http://localhost:3000/jobs to see worker activity, queued jobs, and the configured parallelism.

## Configuration

### Environment Variables

You can customize the system by modifying environment variables in `docker-compose.yml`:

#### Audio Processing
```yaml
MAX_MINUTES_BEFORE_SPLIT: 60  # Split episodes longer than this (minutes)
AUDIO_STORAGE_PATH: /app/data/audio  # Storage path for audio files
```

#### Ollama Models
```yaml
OLLAMA_EMBEDDING_MODEL: nomic-embed-text  # Embedding model
OLLAMA_CHAT_MODEL: llama2  # Chat model for Q&A
```

#### Vector Search
```yaml
VECTOR_SEARCH_TOP_K: 5  # Number of relevant chunks to retrieve
```

#### Job Worker
```yaml
JOB_WORKER_PARALLELISM: 1  # Number of worker threads per instance
JOB_WORKER_POLL_DELAY_MS: 1000  # Delay between empty queue polls
JOB_WORKER_DEQUEUE_TIMEOUT_SECONDS: 5  # Blocking pop timeout for worker threads
JOB_QUEUE_STATUS_LIMIT: 50  # Max queued jobs returned in status view
```

### Changing Models

To use a different chat model:

1. Pull the model:
```bash
docker volume create podcast-indexer_ollama-data
docker run --rm -v podcast-indexer_ollama-data:/root/.ollama ollama/ollama:latest ollama pull mistral
```

2. Update `docker-compose.yml`:
```yaml
environment:
  OLLAMA_CHAT_MODEL: mistral
```

3. Restart the backend:
```bash
docker-compose restart backend
```

## Audio Splitting

Long episodes (exceeding `MAX_MINUTES_BEFORE_SPLIT`) are automatically split into parts:
- Uses ffmpeg for splitting
- Preserves global timestamps across parts
- Each part is transcribed separately
- Timestamps are adjusted to maintain continuity

## API Endpoints

### Podcasts

- `GET /api/podcasts` - List all podcasts
- `GET /api/podcasts/{id}` - Get podcast with episodes
- `POST /api/podcasts` - Add new podcast
  ```json
  {
    "feedUrl": "https://feeds.example.com/podcast.rss",
    "downloadUntilDate": "2024-12-31"
  }
  ```
- `POST /api/podcasts/{id}/sync` - Manually sync episodes

### Q&A

- `POST /api/qa/ask` - Ask a question
  ```json
  {
    "podcastId": 1,
    "question": "What did they say about AI?"
  }
  ```

## Development

### Running Locally (Without Docker)

1. **Start Dependencies**:
```bash
# Start only Postgres, Redis, Ollama, Whisper
docker-compose up postgres redis ollama whisper-service
```

2. **Run Backend**:
```bash
cd backend
./mvnw spring-boot:run
```

3. **Run Frontend**:
```bash
cd frontend
npm install
npm start
```

### Building

**Backend**:
```bash
cd backend
./mvnw clean package
```

**Frontend**:
```bash
cd frontend
npm run build
```

## Troubleshooting

### Whisper Service Fails to Start
- Ensure you have enough disk space for the model download (large-v3 download is several GB and uses ~10 GB VRAM)
- Set `WHISPER_MODEL` in `docker-compose` or your environment to use a smaller model if needed
- Check logs: `docker logs podcast-whisper`

### Ollama Models Not Found
- Pull models manually: `docker volume create podcast-indexer_ollama-data && docker run --rm -v podcast-indexer_ollama-data:/root/.ollama ollama/ollama:latest ollama pull nomic-embed-text`
- Check available models: `docker exec podcast-ollama ollama list`

### Backend Fails to Connect to Services
- Ensure all services are healthy: `docker-compose ps`
- Check backend logs: `docker logs podcast-backend`

### Episode Processing Stuck
- Check job worker logs in backend
- Verify Redis is running: `docker exec podcast-redis redis-cli ping`

### Out of Memory Errors
- Increase Docker memory limit (Docker Desktop settings)
- Use smaller Ollama models (e.g., `phi3` instead of `llama2`)

## Data Persistence

All data is persisted in Docker volumes:
- `postgres-data`: Database with podcasts, episodes, transcripts, embeddings
- `ollama-data`: Downloaded Ollama models
- `audio-data`: Downloaded audio files

To backup:
```bash
docker-compose down
docker run --rm -v podcast-indexer_postgres-data:/data -v $(pwd):/backup ubuntu tar czf /backup/postgres-backup.tar.gz /data
```

To reset everything:
```bash
docker-compose down -v
```

## Performance Considerations

- **Transcription**: Whisper large-v3 is the default for highest accuracy (override with `WHISPER_MODEL`)
  - Use `turbo` for faster transcription with slightly lower accuracy
  - Use `small` or `medium` for lower resource usage
- **Embedding Generation**: Nomic-embed-text is optimized for speed
- **Chat Model**: Smaller models (phi3) respond faster but may be less accurate

## Idempotency & Restart Safety

The system is designed to be idempotent:
- Re-running sync won't re-download/process existing episodes
- Episodes are identified by GUID (or content hash as fallback)
- Processing status prevents re-transcription/re-indexing
- Jobs can be safely restarted

## License

MIT License - see LICENSE file for details

## Contributing

Contributions are welcome! Please open an issue or submit a pull request.
