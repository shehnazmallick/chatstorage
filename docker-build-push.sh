#!/bin/bash

set -e  # Exit on error

# Configuration
DOCKER_USERNAME="${DOCKER_USERNAME:-}"
IMAGE_NAME="chatstorage"
IMAGE_TAG="${IMAGE_TAG:-latest}"
FULL_IMAGE_NAME="${DOCKER_USERNAME}/${IMAGE_NAME}:${IMAGE_TAG}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Helper functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        log_warn "docker-compose not found. Trying 'docker compose'..."
        if ! docker compose version &> /dev/null; then
            log_error "Docker Compose is not installed."
            exit 1
        fi
    fi
    
    log_info "Prerequisites check passed ✓"
}

# Pull base images used in docker-compose.yml
pull_base_images() {
    log_info "Pulling base images from docker-compose.yml..."
    
    local images=(
        "eclipse-temurin:21-jdk-alpine"
        "eclipse-temurin:21-jre-alpine"
        "postgres:16-alpine"
        "redis:7-alpine"
        "adminer:4"
    )
    
    for image in "${images[@]}"; do
        log_info "Pulling ${image}..."
        docker pull "${image}"
    done
    
    log_info "Base images pulled successfully ✓"
}

# Build Docker image
build_image() {
    log_info "Building Docker image: ${FULL_IMAGE_NAME}..."
    
    if docker build -t "${FULL_IMAGE_NAME}" .; then
        log_info "Docker image built successfully ✓"
    else
        log_error "Failed to build Docker image"
        exit 1
    fi
}

# Tag image with additional tags (optional)
tag_image() {
    log_info "Tagging image as '${DOCKER_USERNAME}/${IMAGE_NAME}:latest'..."
    docker tag "${FULL_IMAGE_NAME}" "${DOCKER_USERNAME}/${IMAGE_NAME}:latest"
    log_info "Image tagged successfully ✓"
}

# Push image to Docker Hub
push_image() {
    log_info "Pushing image to Docker Hub: ${FULL_IMAGE_NAME}..."
    
    if docker push "${FULL_IMAGE_NAME}"; then
        log_info "Image pushed successfully ✓"
    else
        log_error "Failed to push image to Docker Hub"
        exit 1
    fi
}

# Display image info
display_image_info() {
    log_info "Image information:"
    docker images | grep "${IMAGE_NAME}" | head -5
}

# Main execution
main() {
    # Validate Docker username is provided
    if [ -z "${DOCKER_USERNAME}" ]; then
        log_error "DOCKER_USERNAME environment variable is required"
        echo ""
        echo "Usage: DOCKER_USERNAME=your_username ./docker-build-push.sh"
        exit 1
    fi
    
    echo "=========================================="
    echo "Docker Build & Push Script"
    echo "=========================================="
    echo "Docker Username: ${DOCKER_USERNAME}"
    echo "Image Name: ${IMAGE_NAME}"
    echo "Image Tag: ${IMAGE_TAG}"
    echo "Full Image: ${FULL_IMAGE_NAME}"
    echo "=========================================="
    echo ""
    
    check_prerequisites
    pull_base_images
    build_image
    run_tests
    tag_image
    push_image
    display_image_info
    
    echo ""
    echo "=========================================="
    log_info "All steps completed successfully!"
    echo "=========================================="
    echo ""
    echo "To use the image:"
    echo "  docker pull ${FULL_IMAGE_NAME}"
    echo "  docker run -p 8080:8080 ${FULL_IMAGE_NAME}"
    echo ""
    echo "Or with docker-compose:"
    echo "  docker-compose up"
    echo ""
}

# Run project tests (Gradle)
run_tests() {
    log_info "Running project tests..."

    if [ -x "./gradlew" ]; then
        log_info "Using project gradle wrapper: ./gradlew clean test"
        if ./gradlew clean test; then
            log_info "Tests passed ✓"
        else
            log_error "Tests failed"
            exit 1
        fi
    elif command -v gradle &> /dev/null; then
        log_info "Using system gradle: gradle clean test"
        if gradle clean test; then
            log_info "Tests passed ✓"
        else
            log_error "Tests failed"
            exit 1
        fi
    else
        log_warn "Gradle wrapper not found and 'gradle' not installed — skipping tests"
    fi
}

# Run main function
main "$@"
