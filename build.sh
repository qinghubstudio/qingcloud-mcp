#!/bin/bash

# ================================
# Qingcloud MCP Service - Build Script
# ================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}Qingcloud MCP Service - Docker Build${NC}"
echo -e "${GREEN}================================${NC}"
echo

# Configuration
IMAGE_NAME="qingcloud-mcp"
IMAGE_TAG="latest"
CONTAINER_NAME="qingcloud-mcp"

# Parse command line arguments
BUILD_ONLY=false
NO_CACHE=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --build-only)
            BUILD_ONLY=true
            shift
            ;;
        --no-cache)
            NO_CACHE=true
            shift
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            echo "Usage: $0 [--build-only] [--no-cache]"
            exit 1
            ;;
    esac
done

# Step 1: Clean up old containers
echo -e "${YELLOW}[1/5] Cleaning up old containers...${NC}"
if docker ps -a | grep -q $CONTAINER_NAME; then
    echo "Stopping and removing existing container..."
    docker stop $CONTAINER_NAME 2>/dev/null || true
    docker rm $CONTAINER_NAME 2>/dev/null || true
    echo -e "${GREEN}✓ Container removed${NC}"
else
    echo "No existing container found"
fi
echo

# Step 2: Build Docker image
echo -e "${YELLOW}[2/5] Building Docker image...${NC}"
if [ "$NO_CACHE" = true ]; then
    echo "Building with --no-cache option..."
    docker build --no-cache -t $IMAGE_NAME:$IMAGE_TAG .
else
    docker build -t $IMAGE_NAME:$IMAGE_TAG .
fi

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Docker image built successfully${NC}"
else
    echo -e "${RED}✗ Failed to build Docker image${NC}"
    exit 1
fi
echo

# Step 3: Check image size
echo -e "${YELLOW}[3/5] Image information:${NC}"
docker images | grep $IMAGE_NAME
echo

if [ "$BUILD_ONLY" = true ]; then
    echo -e "${GREEN}Build complete! (build-only mode)${NC}"
    exit 0
fi

# Step 4: Start container
echo -e "${YELLOW}[4/5] Starting container...${NC}"
docker run -d \
    --name $CONTAINER_NAME \
    -p 8080:8080 \
    -v "$(pwd)/cookies.json:/app/cookies.json" \
    -v "$(pwd)/logs:/app/logs" \
    $IMAGE_NAME:$IMAGE_TAG

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Container started successfully${NC}"
else
    echo -e "${RED}✗ Failed to start container${NC}"
    exit 1
fi
echo

# Step 5: Health check
echo -e "${YELLOW}[5/5] Waiting for service to start...${NC}"
echo "Checking health status (this may take up to 60 seconds)..."

MAX_RETRIES=20
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    sleep 3
    if docker ps | grep -q $CONTAINER_NAME; then
        if curl -s http://localhost:8080/mcp > /dev/null 2>&1; then
            echo -e "${GREEN}✓ Service is healthy and responding${NC}"
            break
        fi
    else
        echo -e "${RED}✗ Container stopped unexpectedly${NC}"
        echo "Showing last 20 lines of logs:"
        docker logs --tail 20 $CONTAINER_NAME
        exit 1
    fi
    
    RETRY_COUNT=$((RETRY_COUNT + 1))
    echo "Waiting... ($RETRY_COUNT/$MAX_RETRIES)"
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo -e "${YELLOW}⚠ Service did not respond within expected time${NC}"
    echo "Showing last 20 lines of logs:"
    docker logs --tail 20 $CONTAINER_NAME
    echo
    echo -e "${YELLOW}The service may still be starting up. Check logs with:${NC}"
    echo "  docker logs -f $CONTAINER_NAME"
fi

echo
echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}Build Complete!${NC}"
echo -e "${GREEN}================================${NC}"
echo
echo "Service URL: http://localhost:8080/mcp"
echo
echo "Useful commands:"
echo "  View logs:      docker logs -f $CONTAINER_NAME"
echo "  Stop service:   docker stop $CONTAINER_NAME"
echo "  Restart:        docker restart $CONTAINER_NAME"
echo "  Remove:         docker rm -f $CONTAINER_NAME"
echo
