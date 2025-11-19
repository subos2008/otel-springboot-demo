# Implementation Plan: Spring Boot 3-Tier Demo App

## Overview
Building a simple Java Spring Boot demo application with three services to demonstrate inter-service HTTP communication, suitable for future OpenTelemetry instrumentation.

This project will contain multiple versions of the application in subdirectories. The first version is `sprintboot-starter`.

## Project Structure (Monorepo)
```
otel-sprintboot/
├── docs/
│   ├── project-genesis-prompt.md
│   └── implementation-plan.md
└── sprintboot-starter/          (Version 1: Basic implementation)
    ├── docker-compose.yml
    ├── README.md
    ├── frontend/                (React SPA - port 3000)
    │   ├── Dockerfile
    │   ├── package.json
    │   └── src/
    ├── backend/                 (Spring Boot - port 3001)
    │   ├── Dockerfile
    │   ├── pom.xml
    │   └── src/
    └── upstream/                (Spring Boot - port 3002)
        ├── Dockerfile
        ├── pom.xml
        └── src/
```

**Note:** Future versions of the app will be added as sibling directories to `sprintboot-starter/`.

## Service Specifications

### 1. Upstream Service (Port 3002)
**Technology Stack:**
- Java 18
- Spring Boot 3.1.x
- Maven
- Spring Boot Actuator

**Endpoints:**
- `GET /api/backend_to_upstream`
  - Returns: `{ "timestamp": "2025-11-19T10:30:45Z", "message": "Hello from upstream", "service": "upstream" }`
  - Purpose: Provides timestamp data to backend service

- `GET /health`
  - Returns: Spring Boot Actuator health status
  - Purpose: Health check endpoint

**Features:**
- Simple REST controller
- Returns current timestamp in ISO-8601 format
- No external dependencies

---

### 2. Backend Service (Port 3001)
**Technology Stack:**
- Java 18
- Spring Boot 3.1.x
- Maven
- Spring Boot Actuator
- RestTemplate or WebClient for HTTP calls

**Endpoints:**
- `GET /api/frontend_to_backend`
  - Receives request from frontend
  - Transforms and forwards to `GET /api/backend_to_upstream` on upstream service
  - Returns: `{ "timestamp": "...", "message": "...", "service": "backend", "upstream": {...} }`

- `POST /api/frontend_to_backend`
  - Similar to GET but demonstrates POST method
  - Forwards to upstream service

- `PUT /api/frontend_to_backend`
  - Demonstrates PUT method

- `DELETE /api/frontend_to_backend`
  - Demonstrates DELETE method

- `GET /health`
  - Returns backend health status
  - Includes upstream connectivity check
  - Returns: `{ "status": "UP", "upstream": { "status": "UP", "responseTime": 45 } }`

**Features:**
- Acts as proxy/middleware between frontend and upstream
- Demonstrates request transformation (different endpoint names)
- Aggregates health information from upstream
- Proper error handling when upstream is down
- CORS configuration for frontend communication

---

### 3. Frontend Service (Port 3000)
**Technology Stack:**
- React 18
- Vite (build tool)
- TypeScript
- Axios or Fetch API

**UI Components:**

1. **HTTP Method Buttons**
   - Four buttons: GET, POST, PUT, DELETE
   - Each button calls `/api/frontend_to_backend` on backend service
   - Shows which method is currently being used
   - Displays response in real-time

2. **Continuous Request Mode**
   - Start/Stop toggle button
   - Input field: interval in seconds (default: 5)
   - Counter: total requests sent
   - Uses GET method for continuous requests
   - Clear visual indicator when running

3. **Response Display Panel**
   - Shows latest response data
   - Displays timestamp from upstream
   - Shows message payload
   - Proof of complete request flow (frontend → backend → upstream → backend → frontend)
   - Request/response time

4. **Health Dashboard**
   - Two status indicators:
     - Backend service health (direct check to `http://localhost:3001/health`)
     - Upstream service health (direct check to `http://localhost:3002/health`)
   - Green/Red/Yellow color indicators
   - Auto-refresh every 5 seconds
   - Last check timestamp
   - Response time for each service

5. **Request History**
   - List of last 10 requests
   - Shows: timestamp, method, status code, response time
   - Scrollable list

**Features:**
- Clean, modern UI (consider Tailwind CSS or Material-UI)
- Error handling and loading states
- Toast notifications for errors
- Responsive design

---

## Docker Configuration

### docker-compose.yml
Located at `sprintboot-starter/docker-compose.yml`:

```yaml
version: '3.8'

services:
  upstream:
    build: ./upstream
    ports:
      - "3002:3002"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:3002/health"]
      interval: 10s
      timeout: 5s
      retries: 3
    environment:
      - SERVER_PORT=3002

  backend:
    build: ./backend
    ports:
      - "3001:3001"
    depends_on:
      - upstream
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:3001/health"]
      interval: 10s
      timeout: 5s
      retries: 3
    environment:
      - SERVER_PORT=3001
      - UPSTREAM_URL=http://upstream:3002

  frontend:
    build: ./frontend
    ports:
      - "3000:3000"
    depends_on:
      - backend
    environment:
      - VITE_BACKEND_URL=http://localhost:3001
      - VITE_UPSTREAM_URL=http://localhost:3002
```

### Dockerfiles
- Multi-stage builds for Spring Boot services (Maven build → JRE runtime)
- Node-based build for React frontend
- Optimized layer caching

---

## Build Tools & Dependencies

### Spring Boot Services (Maven)
- **Spring Boot Starter Web** - REST API support
- **Spring Boot Starter Actuator** - Health endpoints
- **Spring Boot DevTools** - Development convenience
- **Lombok** - Reduce boilerplate code

### Frontend (npm/yarn)
- **React** - UI framework
- **Vite** - Build tool and dev server
- **TypeScript** - Type safety
- **Axios** - HTTP client
- **React Query** (optional) - Data fetching
- **Tailwind CSS** or **Material-UI** - Styling

---

## Implementation Order

1. **Upstream Service**
   - Create Spring Boot project structure
   - Implement `/api/backend_to_upstream` endpoint
   - Implement `/health` endpoint
   - Create Dockerfile
   - Test locally

2. **Backend Service**
   - Create Spring Boot project structure
   - Implement proxy endpoints (`/api/frontend_to_backend`)
   - Add RestTemplate/WebClient configuration
   - Implement request transformation logic
   - Add health check with upstream connectivity
   - Configure CORS
   - Create Dockerfile
   - Test locally with upstream

3. **Frontend Service**
   - Create React + Vite project
   - Build UI components (buttons, forms, displays)
   - Implement API calls to backend
   - Add continuous request functionality
   - Implement health dashboard
   - Add request history
   - Create Dockerfile
   - Test locally with backend

4. **Docker Compose**
   - Create docker-compose.yml
   - Configure networking
   - Add health checks
   - Test full stack

5. **Documentation**
   - Create README.md with:
     - Project overview
     - Prerequisites (Java 18, Node.js, Docker)
     - Setup instructions
     - Running locally (with/without Docker)
     - API documentation
     - Architecture diagram
     - Troubleshooting

---

## Testing Strategy

### Manual Testing Checklist
- [ ] Upstream service responds to requests
- [ ] Backend successfully proxies requests to upstream
- [ ] Frontend buttons trigger correct HTTP methods
- [ ] Continuous request mode works correctly
- [ ] Health checks accurately reflect service status
- [ ] Services recover gracefully when dependencies restart
- [ ] Docker Compose brings up all services
- [ ] CORS is properly configured

### Test Scenarios
1. Start all services → verify health checks are green
2. Stop upstream → verify backend health shows upstream down
3. Stop backend → verify frontend shows backend down
4. Send GET request → verify response shows full chain
5. Start continuous mode → verify requests are sent at interval
6. High-frequency requests (1 second interval) → verify no crashes

---

## Future Enhancements (Post-Implementation)
- Add OpenTelemetry instrumentation
- Add metrics collection
- Add distributed tracing
- Add logging aggregation
- Add request ID propagation
- Add rate limiting
- Add circuit breaker pattern
- Add retry logic with exponential backoff

---

## Notes
- Keep it simple - this is a demo app for instrumentation
- Focus on clear HTTP request flows
- Make it easy to see the request chain
- Services should be independently deployable
- Use Spring Boot conventions and best practices
- Frontend should be intuitive and visual