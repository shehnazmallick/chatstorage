#!/bin/bash

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Detect docker-compose command
DOCKER_COMPOSE_CMD="docker-compose"
if ! command -v docker-compose &> /dev/null; then
    if docker compose version &> /dev/null; then
        DOCKER_COMPOSE_CMD="docker compose"
    else
        log_error "docker-compose not found"
        exit 1
    fi
fi

# Environment file selection
ENV_FILE=".env"
if [[ "${1:-}" == "--prod" ]]; then
    ENV_FILE=".env.prod"
    shift
elif [[ -n "${ENV_FILE_OVERRIDE:-}" ]]; then
    ENV_FILE="${ENV_FILE_OVERRIDE}"
fi

if [[ ! -f "${ENV_FILE}" ]]; then
    log_error "Env file not found: ${ENV_FILE}"
    exit 1
fi

run_compose() {
    ${DOCKER_COMPOSE_CMD} --env-file "${ENV_FILE}" "$@"
}

require_docker_login() {
    local username
    username="$(docker info 2>/dev/null | awk -F': ' '/Username:/ {print $2}')"
    if [[ -n "${username}" ]]; then
        log_info "Docker Hub authenticated as: ${username}"
        return 0
    fi

    log_warn "No Docker Hub login detected in Docker CLI."
    if [[ ! -t 0 ]]; then
        log_error "Non-interactive shell. Please run 'docker login' first."
        exit 1
    fi

    read -r -p "Run 'docker login' now? [Y/n]: " answer
    answer="${answer:-Y}"
    if [[ "${answer}" =~ ^[Yy]$ ]]; then
        docker login
    else
        log_error "Docker login required to continue."
        exit 1
    fi

    username="$(docker info 2>/dev/null | awk -F': ' '/Username:/ {print $2}')"
    if [[ -z "${username}" ]]; then
        log_error "Docker login did not complete successfully."
        exit 1
    fi
    log_info "Docker Hub authenticated as: ${username}"
}

# Main execution
main() {
    echo "=========================================="
    echo "Docker Compose Orchestration Script"
    echo "=========================================="
    echo "Command: ${DOCKER_COMPOSE_CMD}"
    echo "Action: $1"
    echo "Env file: ${ENV_FILE}"
    echo "=========================================="
    echo ""
    
    case "${1:-up}" in
        up)
            require_docker_login
            log_info "Starting all services..."
            run_compose up -d
            log_info "Services started ✓"
            echo ""
            log_info "Waiting for services to be healthy..."
            sleep 10
            run_compose ps
            echo ""
            log_info "Services running:"
            echo "  - App: http://localhost:8080"
            echo "  - Adminer: http://localhost:8081"
            echo "  - PostgreSQL: localhost:5432"
            echo "  - Redis: localhost:6379"
            ;;
        down)
            log_info "Stopping all services..."
            run_compose down
            log_info "Services stopped ✓"
            ;;
        down-clean)
            log_info "Stopping all services and removing volumes..."
            run_compose down -v
            log_info "Services stopped and volumes removed ✓"
            ;;
        logs)
            log_info "Showing logs from all services..."
            run_compose logs -f
            ;;
        logs-app)
            log_info "Showing logs from app service..."
            run_compose logs -f app
            ;;
        status)
            log_info "Checking service status..."
            run_compose ps
            ;;
        build)
            require_docker_login
            log_info "Building services..."
            run_compose build
            log_info "Services built ✓"
            ;;
        rebuild)
            require_docker_login
            log_info "Rebuilding services and starting..."
            run_compose up --build -d
            log_info "Services rebuilt and started ✓"
            ;;
        restart)
            log_info "Restarting all services..."
            run_compose restart
            log_info "Services restarted ✓"
            ;;
        help|--help|-h)
            cat << EOF
Usage: ./docker-compose-run.sh [--prod] [COMMAND]

Commands:
  up              Start all services in background (default)
  down            Stop all services
  down-clean      Stop services and remove volumes
  logs            Show logs from all services
  logs-app        Show logs from app service only
  status          Show running service status
  build           Build images
  rebuild         Rebuild images and start services
  restart         Restart all services
  help            Show this help message

Examples:
  ./docker-compose-run.sh up
  ./docker-compose-run.sh --prod up
  ./docker-compose-run.sh logs-app
  ./docker-compose-run.sh down-clean
  ENV_FILE_OVERRIDE=.env.staging ./docker-compose-run.sh up
EOF
            ;;
        *)
            log_error "Unknown command: $1"
            echo "Run './docker-compose-run.sh help' for usage."
            exit 1
            ;;
    esac
}

main "$@"
