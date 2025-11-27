# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an OpenTelemetry demonstration project featuring multiple versions of a Spring Boot microservices application. The project is designed to showcase observability patterns, distributed tracing, and inter-service HTTP communication.

## Repository Structure

```
otel-sprintboot/
├── docs/                          # Project documentation
│   ├── project-genesis-prompt.md  # Original requirements
│   └── implementation-plan.md     # Technical specifications
├── sprintboot-starter/            # Version 1: Baseline (no instrumentation)
│   ├── frontend/                  # React SPA (port 3000)
│   ├── backend/                   # Spring Boot proxy service (port 3001)
│   ├── upstream/                  # Spring Boot data service (port 3002)
│   └── docker-compose.yml
└── [future versions will go here]
```

## Architecture

### Request Flow
```
User Browser → Frontend (React) → Backend (Spring Boot) → Upstream (Spring Boot)
   Port 3000       HTTP             Port 3001              Port 3002
```

### Service Details

#### Frontend (sprintboot-starter/frontend/)
- **Tech:** React 18, TypeScript, Vite, Axios
- **Port:** 3000
- **Purpose:** User interface for testing HTTP methods and monitoring service health
- **Features:**
  - HTTP method buttons (GET, POST, PUT, DELETE)
  - Continuous request mode with configurable intervals
  - Real-time health monitoring (checks both backend and upstream)
  - Response display with full request chain
  - Request history (last 10)

#### Backend (sprintboot-starter/backend/)
- **Tech:** Spring Boot 2.7.6, Java 17, Maven, Apache Camel 3.11.5
- **Port:** 3001
- **Purpose:** Middleware/proxy that transforms and forwards requests
- **Key Files:**
  - `BackendApplication.java` - Main application class, RestTemplate bean
  - `BackendController.java` - Proxy endpoints with CORS enabled
  - `UpstreamHealthIndicator.java` - Custom health check that monitors upstream
  - `CorsConfig.java` - Global CORS configuration
  - `application.properties` - Configuration including actuator CORS settings
- **Endpoints:**
  - `GET/POST/PUT/DELETE /api/frontend_to_backend` - Proxy to upstream
  - `GET /actuator/health` - Health check with upstream connectivity status
- **Environment Variables:**
  - `UPSTREAM_SERVICE_URL` - URL of upstream service (default: http://localhost:3002)

#### Upstream (sprintboot-starter/upstream/)
- **Tech:** Spring Boot 2.7.6, Java 17, Maven, Apache Camel 3.11.5
- **Port:** 3002
- **Purpose:** Simple data service that returns timestamps
- **Key Files:**
  - `UpstreamApplication.java` - Main application class
  - `UpstreamController.java` - REST endpoints
  - `CorsConfig.java` - Global CORS configuration
  - `application.properties` - Configuration including actuator CORS settings
- **Endpoints:**
  - `GET/POST/PUT/DELETE /api/backend_to_upstream` - Returns timestamp data
  - `GET /actuator/health` - Health check

## Common Development Tasks

### Starting Services Locally

**Prerequisites:**
- Java 17 (install via Homebrew: `brew install openjdk@17`)
- Maven 3.9+ (install via Homebrew: `brew install maven`)
- Node.js 18+ (install via Homebrew: `brew install node`)

**Start All Services (in separate terminals):**

```bash
# Terminal 1 - Upstream
cd sprintboot-starter/upstream
export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"
export JAVA_HOME="/opt/homebrew/opt/openjdk@17"
mvn spring-boot:run

# Terminal 2 - Backend
cd sprintboot-starter/backend
export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"
export JAVA_HOME="/opt/homebrew/opt/openjdk@17"
mvn spring-boot:run

# Terminal 3 - Frontend
cd sprintboot-starter/frontend
npm install  # only needed first time
npm run dev
```

**Or use Docker Compose:**
```bash
cd sprintboot-starter
docker-compose up --build
```

### Testing the Application

**Manual Testing:**
```bash
# Test upstream directly
curl http://localhost:3002/api/backend_to_upstream

# Test backend (which calls upstream)
curl http://localhost:3001/api/frontend_to_backend

# Check health endpoints
curl http://localhost:3001/actuator/health
curl http://localhost:3002/actuator/health

# Access frontend
open http://localhost:3000
```

**Expected Response Chain:**
```json
{
  "service": "backend",
  "method": "GET",
  "upstream": {
    "service": "upstream",
    "timestamp": "2025-11-19T03:00:00Z",
    "message": "Hello from upstream"
  }
}
```

## Important Configuration Details

### CORS Configuration

CORS is configured in **two places** for each Spring Boot service:

1. **CorsConfig.java** - Global CORS for all endpoints
2. **application.properties** - Actuator-specific CORS settings

```properties
# Actuator CORS (required for health checks from frontend)
management.endpoints.web.cors.allowed-origins=*
management.endpoints.web.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
management.endpoints.web.cors.allowed-headers=*
```

**Why both?** The `CorsConfig.java` handles application endpoints, but Spring Boot Actuator endpoints need separate configuration in `application.properties`.

### Spring Boot DevTools

Both backend and upstream services include `spring-boot-devtools`, which enables:
- Automatic application restart on file changes
- Live reload capabilities
- Enhanced development experience

To trigger a reload, simply save any `.java` file or touch the application file:
```bash
touch src/main/java/com/demo/backend/BackendApplication.java
```

### Required Dependency Versions

The project uses the following specific versions:
- **Java:** 17
- **Spring Boot:** 2.7.6
- **Apache Camel:** 3.11.5
- **Camel Spring Boot:** 3.11.5
- **Camel CXF:** 3.11.5
- **CXF WS Security:** 3.5.4

### Port Configuration

- Frontend: 3000 (configurable in `vite.config.ts`)
- Backend: 3001 (configurable in `application.properties`)
- Upstream: 3002 (configurable in `application.properties`)

## Making Changes

### Adding New Endpoints

**Backend/Upstream:**
1. Add method to respective `*Controller.java`
2. Use `@GetMapping`, `@PostMapping`, etc.
3. DevTools will auto-reload the service
4. Test with curl or frontend

**Frontend:**
1. Modify `src/App.tsx` to add UI components
2. Vite HMR will auto-reload the browser
3. Use Axios for HTTP calls

### Modifying Health Checks

**Backend Health Check with Upstream Monitoring:**
- Edit `UpstreamHealthIndicator.java`
- Returns `Health.up()` or `Health.down()` based on upstream connectivity
- Automatically included in `/actuator/health` response

### Changing Service Configuration

**Backend:**
- `application.properties` - Port, upstream URL, actuator settings
- Environment variable: `UPSTREAM_SERVICE_URL`

**Frontend:**
- `vite.config.ts` - Dev server port
- Environment variables: `VITE_BACKEND_URL`, `VITE_UPSTREAM_URL`

## Troubleshooting

### CORS Errors in Browser

**Symptom:** Browser console shows "Origin not allowed" errors
**Solution:**
1. Verify CORS config in both `CorsConfig.java` AND `application.properties`
2. Restart Spring Boot services (DevTools may not pick up property changes)
3. Check browser network tab for CORS headers

### Services Can't Connect

**Symptom:** Backend can't reach upstream
**Solution:**
1. Check `UPSTREAM_SERVICE_URL` environment variable
2. Verify upstream is running: `curl http://localhost:3002/actuator/health`
3. Check firewall/network settings

### Maven Build Fails

**Symptom:** Maven can't download dependencies
**Solution:**
1. Check internet connection
2. Clear Maven cache: `rm -rf ~/.m2/repository`
3. Verify Java version: `java -version` (should be 17)

### Frontend Shows Services as DOWN

**Symptom:** Health dashboard shows red indicators
**Solution:**
1. Check if backend/upstream are running
2. Verify CORS is enabled for actuator endpoints
3. Open browser dev tools and check for CORS errors
4. Refresh the page after services start

## Project Philosophy

### Simplicity First
This is a **demo application** designed for learning and experimentation. Keep implementations simple and focused on demonstrating concepts, not production-readiness.

### Observable by Design
The architecture is specifically designed to showcase observability:
- Clear service boundaries
- HTTP-based communication (easy to trace)
- Health checks at every level
- Request transformation (demonstrates context propagation)

### Version Strategy
Each version in this repo should build on the previous:
- `sprintboot-starter` - Baseline with no instrumentation
- Future versions will add OpenTelemetry, custom instrumentation, etc.

## Testing Philosophy

This project focuses on **manual testing** and **demonstration**:
- No unit tests required (this is a demo)
- Manual verification via browser and curl
- Health checks provide runtime validation
- Request history shows proof of functionality

## Code Style

### Java (Spring Boot)
- Use Spring Boot conventions
- Keep controllers simple and focused
- Configuration via `application.properties` over Java config when possible
- Minimal dependencies

### React (Frontend)
- Functional components with hooks
- TypeScript for type safety
- Inline styles or simple CSS (no complex styling frameworks)
- Axios for HTTP requests

## Future Versions

When creating new versions:
1. Copy `sprintboot-starter/` to new directory (e.g., `sprintboot-instrumented/`)
2. Add instrumentation/features incrementally
3. Update root `README.md` to document new version
4. Keep versions independent but similar in structure
5. Document differences in version-specific README

## Quick Reference

**Service URLs:**
- Frontend: http://localhost:3000
- Backend API: http://localhost:3001/api/frontend_to_backend
- Backend Health: http://localhost:3001/actuator/health
- Upstream API: http://localhost:3002/api/backend_to_upstream
- Upstream Health: http://localhost:3002/actuator/health

**Key Files:**
- `/docs/project-genesis-prompt.md` - Original requirements
- `/docs/implementation-plan.md` - Detailed specifications
- `sprintboot-starter/README.md` - Setup instructions
- `CLAUDE.md` - This file (AI assistant guide)

## Notes for AI Assistants

- Always check service status before making changes
- Remember CORS configuration has two parts (Java + properties)
- DevTools enables hot reload, but property changes need full restart
- The main request flow is: Frontend → Backend → Upstream
- Health checks are critical for demo purposes - keep them working
- When adding features, maintain the simple, educational nature of the code
- Test changes manually using curl and the browser UI
- Keep documentation fairly succinct, suitable for experienced developers and AIs to read
