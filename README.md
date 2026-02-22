# Chat Storage Microservice

Production-ready backend microservice to store RAG chatbot chat histories with secure multi-user API key auth, Redis-backed distributed rate limiting, and session/message management APIs.

## What This App Does

- Stores chat sessions per user.
- Stores chat messages inside each session.
- Supports session rename, favorite toggle, and deletion.
- Supports paginated session/message retrieval.
- Secures APIs with API key authentication.
- Supports per-user keys managed by an admin key.

## Architecture Summary

- Runtime: Java 21 + Spring Boot
- Data store: PostgreSQL (sessions, messages, API key metadata)
- Distributed rate limiting: Redis + Bucket4j (token bucket)
- API docs: Swagger/OpenAPI (`/swagger-ui.html`)
- Health checks: Actuator (`/actuator/health`, liveness, readiness)

## Authentication Model

There are two key types:

1. Admin key (bootstrap key)
- Comes from environment variable `ADMIN_API_KEY`.
- Not created via API.
- Used only for API key management endpoints.

2. User API key
- Created by admin through API.
- Returned once in plaintext at creation time.
- Stored in DB as hash (never plaintext).
- Enforced as one key per `userId` (unique). Creating again rotates/replaces the existing key.

### How Admin Is Created

Admin is configured by deployment, not by endpoint:

- Set `ADMIN_API_KEY` in `.env` / `.env.prod` before app startup.
- Example: `ADMIN_API_KEY=super-long-random-admin-secret`

This is the admin identity bootstrap.

## Headers and Auth Requirements

### For key management endpoints (`/api/v1/api-keys/**`)

Required headers:
- `X-Admin-Key: <ADMIN_API_KEY>`
- `Content-Type: application/json` (for request bodies)

### For chat/session/message endpoints (`/api/v1/**` except `/api-keys/**`)

Required headers:
- `X-API-Key: <user-api-key>`
- `Content-Type: application/json` (for request bodies)

## Authorization

- Any valid active user API key can access chat/session/message endpoints.
- API key management endpoints remain admin-only via `X-Admin-Key`.

## Sender Types in Messages

Message payload field `sender` supports:

- `USER`: message from human user
- `ASSISTANT`: message from model/assistant
- `SYSTEM`: system-generated metadata/instruction message

This is message semantics, not account roles.

## API Key Lifecycle (Admin Flow)

### 1. Create a user API key

`POST /api/v1/api-keys`

Request body:

```json
{
  "userId": "user-123",
  "name": "web-client-prod"
}
```

Response includes plaintext key once:

```json
{
  "id": "...",
  "userId": "user-123",
  "name": "web-client-prod",
  "apiKey": "csk_<prefix>.<secret>",
  "createdAt": "2026-02-22T...Z"
}
```

### 2. List keys for a user

`GET /api/v1/api-keys?userId=user-123`

### 3. Revoke key

`DELETE /api/v1/api-keys/{apiKeyId}`

## Chat API Contracts

### Create session

`POST /api/v1/sessions`

```json
{
  "title": "My RAG Chat"
}
```

Notes:
- If title is blank or omitted, defaults to `New Chat`.
- User ID is inferred from authenticated key owner (not client-supplied).

### List sessions (paginated)

`GET /api/v1/sessions?page=0&size=20&sort=updatedAt,desc&favorite=true`

### Rename session

`PATCH /api/v1/sessions/{sessionId}/rename`

```json
{
  "title": "Renamed Session"
}
```

### Favorite/unfavorite session

`PATCH /api/v1/sessions/{sessionId}/favorite`

```json
{
  "favorite": true
}
```

### Delete session

`DELETE /api/v1/sessions/{sessionId}`

### Add message

`POST /api/v1/sessions/{sessionId}/messages`

```json
{
  "sender": "USER",
  "content": "What is RAG?",
  "retrievedContext": "Optional retriever context snippet"
}
```

### List messages (paginated)

`GET /api/v1/sessions/{sessionId}/messages?page=0&size=50&sort=createdAt,asc`

Pagination limits:
- Sessions: max `size=100`
- Messages: max `size=200`

## End-to-End Example (cURL)

### Create a user key as admin

```bash
curl -X POST http://localhost:8080/api/v1/api-keys \
  -H "X-Admin-Key: ${ADMIN_API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-123",
    "name": "web-client-dev"
  }'
```

### Use returned `apiKey` for chat APIs

```bash
curl -X POST http://localhost:8080/api/v1/sessions \
  -H "X-API-Key: csk_xxx.yyy" \
  -H "Content-Type: application/json" \
  -d '{"title":"Interview Prep"}'
```

## Rate Limiting

- Token bucket with Redis backend (distributed-safe across instances).
- Client identity key is SHA-256 fingerprint of API key + client IP.
- Configurable via env vars:
  - `RATE_LIMIT_PER_MINUTE`
  - `RATE_LIMIT_WINDOW_SECONDS`
  - `RATE_LIMIT_FAIL_OPEN`

Behavior:
- Exceeded rate -> `429 Too Many Requests` + `Retry-After`
- Redis unavailable and fail-open false -> `503 Service Unavailable`

## Error Response Format

All errors are returned as structured JSON:

```json
{
  "timestamp": "2026-02-22T...Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/sessions",
  "details": ["field message"]
}
```

## Environment and Running

## Env Selection

- `./docker-compose-run.sh up` uses `.env`.
- `./docker-compose-run.sh --prod up` uses `.env.prod`.
- Optional override: `ENV_FILE_OVERRIDE=.env.staging ./docker-compose-run.sh up`.
- Script prompts for `docker login` if Docker Hub auth is missing.

## Required env variables

At minimum, set these non-empty values:
- `ADMIN_API_KEY`
- `API_KEY_PEPPER`
- `DB_USERNAME`
- `DB_PASSWORD`

See templates:
- `.env.example`
- `.env.prod.example`

## CORS Configuration (Security)

CORS is enforced server-side and is configurable through env vars:

- `CORS_ALLOWED_ORIGINS` (recommended explicit frontend domains)
- `CORS_ALLOWED_METHODS`
- `CORS_ALLOWED_HEADERS`
- `CORS_EXPOSED_HEADERS` (default: `X-Request-Id,Retry-After`)
- `CORS_ALLOW_CREDENTIALS` (default: `false`)
- `CORS_MAX_AGE_SECONDS` (default: `3600`)

Security recommendation:
- In production, do not use wildcard `*` origins. Set exact frontend origins.

## Run with Docker

```bash
cp .env.example .env
./docker-compose-run.sh rebuild
```

Prod:

```bash
cp .env.prod.example .env.prod
./docker-compose-run.sh --prod rebuild
```

Services:
- API: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`
- Adminer: `http://localhost:8081`
- Redis: `localhost:6379`

## Build and Push Docker Image

The `docker-build-push.sh` script automates building, testing, and pushing the Docker image to Docker Hub.

### Prerequisites

- Docker installed and logged in: `docker login`
- Docker username set (defaults to `shehnaz`)

### Usage

**Default (uses `shehnaz/chatstorage:latest`):**

```bash
./docker-build-push.sh
```

**Custom Docker Hub username:**

```bash
DOCKER_USERNAME=your_username ./docker-build-push.sh
```

**Custom image tag:**

```bash
IMAGE_TAG=v1.0.0 ./docker-build-push.sh
```

### What the script does:

1. ✓ Checks Docker and Docker Compose prerequisites
2. ✓ Pulls base images (Java, PostgreSQL, Redis, Adminer)
3. ✓ Builds the Docker image
4. ✓ Runs all unit tests
5. ✓ Tags the image as `latest`
6. ✓ Pushes to Docker Hub
7. ✓ Displays image info

### Example: Push with custom tag

```bash
DOCKER_USERNAME=shehnaz IMAGE_TAG=v2.0.0 ./docker-build-push.sh
```

This will build and push `shehnaz/chatstorage:v2.0.0` to Docker Hub.

## Testing and Coverage

```bash
./gradlew test
./gradlew jacocoTestCoverageVerification
```

Coverage gate:
- 90% line coverage on core logic classes configured in Gradle.

## Final Summary

- Admin is created by deployment config via `ADMIN_API_KEY` env var.
- Admin creates user keys via `POST /api/v1/api-keys` with `X-Admin-Key` header.
- User calls chat APIs with `X-API-Key` header.
- Message format uses `sender/content/retrievedContext`; sender can be `USER`, `ASSISTANT`, `SYSTEM`.
- App is distributed-ready with Redis token-bucket rate limiting and PostgreSQL persistence.
- Swagger/OpenAPI includes endpoint-level docs for request/response models, status codes, and auth headers (`X-API-Key`, `X-Admin-Key`).
