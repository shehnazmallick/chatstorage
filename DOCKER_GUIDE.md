# Docker Setup & Deployment Guide

This directory contains scripts to build, push, and run the ChatStorage application using Docker.

## Scripts Overview

### 1. `docker-build-push.sh` — Build and Push Image to Docker Hub

Automates the entire workflow: check prerequisites, pull base images, build the image, and push to Docker Hub.

**Usage:**

```bash
# Set Docker username (default: your_dockerhub_username)
export DOCKER_USERNAME=your_dockerhub_username

# Run the script
./docker-build-push.sh

# Or specify tag
IMAGE_TAG=v1.0.0 ./docker-build-push.sh
```

**What it does:**
1. ✓ Checks Docker and Docker Compose installation
2. ✓ Validates Docker login credentials
3. ✓ Pulls all base images (Java, PostgreSQL, Redis, Adminer)
4. ✓ Builds the application image
5. ✓ Tags the image as latest
6. ✓ Pushes to Docker Hub (your_dockerhub_username/chatstorage:latest)

**Prerequisites:**
- Docker installed and running
- Logged into Docker Hub: `docker login -u your_dockerhub_username`

**Environment Variables:**
- `DOCKER_USERNAME` — Docker Hub username (default: your_dockerhub_username)
- `IMAGE_TAG` — Image tag (default: latest)

---

### 2. `docker-compose-run.sh` — Orchestrate Services

Easy commands to start, stop, and manage all services (app, PostgreSQL, Redis, Adminer).

**Usage:**

```bash
./docker-compose-run.sh [COMMAND]
```

**Available Commands:**

| Command | Action |
|---------|--------|
| `up` | Start all services in background (default) |
| `down` | Stop all services |
| `down-clean` | Stop services and remove volumes (clean data) |
| `logs` | Show logs from all services |
| `logs-app` | Show logs from app service only |
| `status` | Show running service status |
| `build` | Build images |
| `rebuild` | Rebuild images and start services |
| `restart` | Restart all services |
| `help` | Show help message |

**Examples:**

```bash
# Start services
./docker-compose-run.sh up

# View app logs
./docker-compose-run.sh logs-app

# Stop and clean up
./docker-compose-run.sh down-clean

# Rebuild and start
./docker-compose-run.sh rebuild
```

**Services Started:**
- **App** — Spring Boot application on `http://localhost:8080`
- **PostgreSQL** — Database on `localhost:5432`
- **Redis** — Cache & rate-limiter on `localhost:6379`
- **Adminer** — Database UI on `http://localhost:8081`

---

## Full Workflow

### First-time Setup

```bash
# 1. Ensure Docker is running and you're logged in
docker login -u your_dockerhub_username

# 2. Build image and push to Docker Hub
./docker-build-push.sh

# 3. Start local services with docker-compose
./docker-compose-run.sh up

# 4. Verify services are running
./docker-compose-run.sh status
```

### Daily Development

```bash
# Start services
./docker-compose-run.sh up

# View logs
./docker-compose-run.sh logs-app

# Stop when done
./docker-compose-run.sh down
```

### Deploy Updates

```bash
# Rebuild and push new version
IMAGE_TAG=v1.0.1 ./docker-build-push.sh

# Restart local environment with new image
./docker-compose-run.sh rebuild
```
