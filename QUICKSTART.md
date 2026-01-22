# Quick Start Guide

## Setup (5 minutes)

```bash
# 1. Clone the repository
git clone https://github.com/tmseidel/podcast-indexer.git
cd podcast-indexer

# 2. Run the setup script (this will take 10-15 minutes for model downloads)
./setup.sh

# 3. Access the application
# Frontend: http://localhost:3000
# Backend API: http://localhost:8080
```

## Basic Usage

### Add Your First Podcast

1. Go to http://localhost:3000
2. Paste a podcast RSS feed URL (e.g., `https://feeds.simplecast.com/54nAGcIl`)
3. Click "Add Podcast"
4. System automatically discovers and processes episodes

### Monitor Processing

1. Click on a podcast to view episodes
2. Watch the status badges change:
   - DISCOVERED → DOWNLOADING → DOWNLOADED → TRANSCRIBING → TRANSCRIBED → INDEXING → INDEXED
3. Episodes are processed in the background
4. Page auto-refreshes every 10 seconds

### Ask Questions

1. Navigate to "Ask Question" page
2. Select a podcast from dropdown
3. Type your question (e.g., "What topics were discussed?")
4. View answer with timestamped citations
5. Click citations to see episode details

## Common Commands

```bash
# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Restart a specific service
docker-compose restart backend

# Check service health
docker-compose ps

# Pull a different chat model
docker exec podcast-ollama ollama pull mistral
```

## Troubleshooting

**Problem**: Services won't start
```bash
# Check Docker is running
docker info

# Check for port conflicts
lsof -i :3000
lsof -i :8080
```

**Problem**: Episode processing stuck
```bash
# Check Redis
docker exec podcast-redis redis-cli ping

# View backend logs
docker logs podcast-backend -f
```

**Problem**: Out of memory
- Increase Docker memory limit in Docker Desktop
- Use smaller Ollama model: `docker exec podcast-ollama ollama pull phi3`

## Tips

- First transcription will be slow (Whisper model download ~500MB)
- Each episode takes 5-10 minutes to fully process
- Use shorter test podcasts first
- Citations include timestamps - click to see exact moment discussed
- Re-syncing won't re-process already completed episodes

## Example Podcast Feeds

Try these to test:

- Lex Fridman: `https://lexfridman.com/feed/podcast/`
- The Changelog: `https://changelog.com/podcast/feed`
- Software Engineering Daily: `https://softwareengineeringdaily.com/feed/podcast/`

## Architecture

```
User → Frontend (React) → Backend (Spring Boot) → Services
                              ↓
                         PostgreSQL + pgvector
                              ↓
                         Redis Queue
                              ↓
                    ┌─────────┴──────────┐
                    ↓                    ↓
              Whisper Service      Ollama Service
              (Transcription)   (Embeddings & Chat)
```
