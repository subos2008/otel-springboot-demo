# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an OpenTelemetry demonstration project featuring multiple versions of a Spring Boot backend service. **Focus:** Demonstrating how to instrument and observe HTTP requests flowing **into** and **out of** backend services. The upstream service is a simple echo service that exists to enable tracing of outbound calls from backends.

## Repository Structure

```
otel-sprintboot/
├── frontend/              # React UI with backend selector (port 3000)
├── upstream/              # Spring Boot echo service (port 3002)
├── backends/
│   ├── springboot-starter/    # Spring Boot Starter instrumentation
│   │   ├── rest-app/          # Standard REST (port 3010)
│   │   └── camel-rest-app/    # Apache Camel routing (port 3012)
│   └── otel-java-agent/       # OTEL Java Agent instrumentation
│       └── rest-app/          # Standard REST (port 3011)
├── otel/                  # OpenTelemetry Java agent JAR
└── docs/                  # Documentation
```

## Architecture

### Request Flow
```
Frontend (3000) → Backend Service → Upstream (3002)
                      ↓
                  [3 versions:]
                  - Starter REST (3010)
                  - Agent REST (3011)
                  - Camel REST (3012)
```

### Service Details

#### Frontend (frontend/)
- **Tech:** React 18, TypeScript, Vite, Axios
- **Port:** 3000
- **Purpose:** UI with backend selector to toggle between 3 instrumentation approaches
- **Features:** HTTP method buttons, continuous request mode, health monitoring, request history

#### Backend Services (backends/)
Three versions demonstrating different OpenTelemetry instrumentation approaches:

**Starter REST** (`backends/springboot-starter/rest-app/`):
- Port 3010
- Spring Boot Starter with manual instrumentation
- @WithSpan annotations, RestTemplate with context propagation
- Key files: `BackendController.java`, `BackendApplication.java`, `UpstreamHealthIndicator.java`

**Agent REST** (`backends/otel-java-agent/rest-app/`):
- Port 3011
- OTEL Java Agent with automatic instrumentation
- Zero-code instrumentation via `-javaagent` flag
- Key files: `BackendController.java`, `BackendApplication.java`

**Camel REST** (`backends/springboot-starter/camel-rest-app/`):
- Port 3012
- Apache Camel 3.11.5 with Spring Boot Starter instrumentation
- Enterprise Integration Patterns, ProducerTemplate routing, camel-opentelemetry
- Key files: `BackendController.java`, `ProxyRoute.java`, `BackendApplication.java`

All backends:
- Proxy `GET/POST/PUT/DELETE /api/frontend_to_backend` to upstream
- Expose `/actuator/health` with upstream connectivity checks
- Use `UPSTREAM_SERVICE_URL` environment variable

#### Upstream (upstream/)
- **Tech:** Spring Boot 2.7.6, Java 17
- **Port:** 3002
- **Purpose:** Simple echo service for demonstrating outbound call tracing
- **Endpoints:** `GET/POST/PUT/DELETE /api/backend_to_upstream`, `/actuator/health`

## Common Development Tasks

### Starting Services

**Recommended: Docker Compose** (from project root):
```bash
docker-compose up --build
# Or detached: docker-compose up -d --build
```

This starts all services with hot reload enabled. See [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) for development workflow.

### Testing the Application

```bash
# Access frontend (best way to test)
open http://localhost:3000

# Or test backends directly
curl http://localhost:3010/api/frontend_to_backend  # Starter REST
curl http://localhost:3011/api/frontend_to_backend  # Agent REST
curl http://localhost:3012/api/frontend_to_backend  # Camel REST

# Check health
curl http://localhost:3010/actuator/health
```

## Important Configuration Details

### CORS Configuration

CORS requires configuration in **two places**:
1. **CorsConfig.java** - For application endpoints
2. **application.properties** - For actuator endpoints (health checks)

Both must allow `*` origins for the frontend to work.

### Hot Reload

All services use hot reload (Spring DevTools + Vite HMR). Changes to `.java` files trigger automatic recompilation (~2-5 seconds).

### Versions

- Java 17, Spring Boot 2.7.6, Apache Camel 3.11.5
- OpenTelemetry Instrumentation BOM 2.22.0

## Making Changes

### Adding Endpoints
- Edit `*Controller.java`, add `@GetMapping`/`@PostMapping`, save → DevTools auto-reloads
- For frontend: edit `App.tsx` → Vite HMR reloads instantly

### Configuration
- OpenTelemetry settings: `.env` file (see [docs/OTEL_SETUP.md](docs/OTEL_SETUP.md))
- Service ports: `application.properties`
- Upstream URL: `UPSTREAM_SERVICE_URL` environment variable

## Troubleshooting

- **CORS errors:** Check both `CorsConfig.java` and `application.properties` have CORS enabled
- **Services can't connect:** Verify `UPSTREAM_SERVICE_URL` and check services are running via health endpoints
- **Maven fails:** Check internet connection, clear cache (`rm -rf ~/.m2/repository`), verify Java 17
- **Health dashboard shows DOWN:** Wait for services to start (~30s first time), check browser console for errors

## Project Philosophy

**Purpose:** Demo application for learning OpenTelemetry instrumentation patterns. Focus on simplicity and clear demonstration of concepts.

**Key Principles:**
- Keep code simple and educational (not production-ready)
- Demonstrate observability: clear service boundaries, traceable HTTP flows
- Manual testing via browser/curl (no unit tests needed)
- Add extensive comments about OpenTelemetry design decisions, gotchas, and special cases

## Quick Reference

**Service URLs:**
- Frontend: http://localhost:3000 (select between 3 backends)
- Starter REST: http://localhost:3010/api/frontend_to_backend
- Agent REST: http://localhost:3011/api/frontend_to_backend
- Camel REST: http://localhost:3012/api/frontend_to_backend
- Upstream: http://localhost:3002/api/backend_to_upstream

**Key Documentation:**
- [README.md](README.md) - Project overview
- [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) - Hot reload workflow
- [docs/OTEL_SETUP.md](docs/OTEL_SETUP.md) - OpenTelemetry configuration

## Notes for AI Assistants

- **Focus:** This demonstrates backend service instrumentation (inbound/outbound HTTP tracing)
- **CORS:** Requires config in both `CorsConfig.java` and `application.properties`
- **Hot reload:** DevTools auto-reloads on `.java` changes, but `.properties` changes need restart
- **Testing:** Use frontend at http://localhost:3000 or curl commands
- **Documentation:** Keep succinct, suitable for experienced developers
- **Code comments:** Add extensive comments about OpenTelemetry design decisions, gotchas, and special cases
