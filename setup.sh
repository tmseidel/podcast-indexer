#!/bin/bash

echo "======================================"
echo "Podcast Indexer - Initial Setup"
echo "======================================"
echo ""

# Check if docker is running
if ! docker info > /dev/null 2>&1; then
    echo "Error: Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null; then
    echo "Error: docker-compose is not installed. Please install docker-compose and try again."
    exit 1
fi

docker volume create podcast-indexer_ollama-data 2>/dev/null

echo "Pulling required Ollama models..."
echo "This may take a while depending on your internet connection..."
echo ""

pull_ollama_model() {
    local description=$1
    local model=$2
    echo "Pulling ${description}..."
    docker run --rm -v podcast-indexer_ollama-data:/root/.ollama ollama/ollama:latest ollama pull "${model}" || exit 1
    echo ""
}

OLLAMA_EMBEDDING_MODEL=${OLLAMA_EMBEDDING_MODEL:-nomic-embed-text}
OLLAMA_CHAT_MODEL=${OLLAMA_CHAT_MODEL:-llama2}

pull_ollama_model "embedding model (${OLLAMA_EMBEDDING_MODEL})" "${OLLAMA_EMBEDDING_MODEL}"
pull_ollama_model "chat model (${OLLAMA_CHAT_MODEL})" "${OLLAMA_CHAT_MODEL}"
echo "Starting all services..."
docker-compose up -d

echo ""
echo "Waiting for services to be healthy..."
sleep 30

echo "======================================"
echo "Setup complete!"
echo "======================================"
echo ""
echo "Access the application:"
echo "  Frontend: http://localhost:3000"
echo "  Backend API: http://localhost:8080"
echo ""
echo "To view logs:"
echo "  docker-compose logs -f"
echo ""
echo "To stop all services:"
echo "  docker-compose down"
echo ""
