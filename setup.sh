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

docker volume create podcast-indexer_ollama-data >/dev/null

echo "Pulling required Ollama models..."
echo "This may take a while depending on your internet connection..."
echo ""

echo "Pulling embedding model (nomic-embed-text)..."
docker run --rm -v podcast-indexer_ollama-data:/root/.ollama ollama/ollama:latest ollama pull nomic-embed-text

echo ""
echo "Pulling chat model (llama2)..."
docker run --rm -v podcast-indexer_ollama-data:/root/.ollama ollama/ollama:latest ollama pull llama2

echo ""
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
