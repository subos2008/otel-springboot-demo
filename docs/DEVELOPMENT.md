# Development Guide - Hot Reload with Docker

This guide explains how to use Docker with hot reload enabled for faster development.

## Quick Start

```bash
# Start all services with hot reload enabled (from project root)
docker-compose up --build

# Or run in detached mode
docker-compose up -d --build
```

This will start:
- **Frontend** (port 3000) - React with Vite HMR
- **Upstream** (port 3002) - Spring Boot with DevTools
- **Backend Starter** (port 3010) - Spring Boot Starter (manual instrumentation)
- **Backend Agent** (port 3011) - OTEL Java Agent (automatic instrumentation)

## How Hot Reload Works

### Spring Boot Services (Backends & Upstream)
- **Technology**: Spring Boot DevTools + Maven
- **What's mounted**: Source code (`src/`) and `pom.xml`
- **Reload trigger**: When you save a `.java` file
- **Reload time**: ~2-5 seconds (Maven recompiles and Spring Boot restarts)
- **Agent behavior** (backend-agent only): Automatically re-instruments on restart

### Frontend (React)
- **Technology**: Vite HMR (Hot Module Replacement)
- **What's mounted**: Source code (`src/`), `index.html`, `vite.config.ts`
- **Reload trigger**: When you save any source file
- **Reload time**: Instant (<1 second)

## Making Changes

### Edit Java Code (Backends/Upstream)

1. **Edit a file** - Example: `backends/springboot-agent/src/main/java/com/demo/backend/BackendController.java`
   ```java
   @GetMapping("/api/frontend_to_backend")
   public Map<String, Object> handleGet() {
       // Make your changes here
       String message = "Updated message!"; // <- Change this
       ...
   }
   ```

2. **Save the file** - The following happens automatically:
   - Maven detects the change
   - Compiles the modified class
   - Spring Boot DevTools restarts the application context
   - Container continues running (no full restart)

3. **Check logs** - See the reload happen:
   ```bash
   docker-compose logs -f backend-agent
   ```
   You'll see: `Restarting due to 1 class path change...`

4. **Test the change**:
   ```bash
   curl http://localhost:3011/api/frontend_to_backend
   ```

### Edit React Code (Frontend)

1. **Edit a file** - Example: `frontend/src/App.tsx`
2. **Save the file** - Vite HMR updates instantly in the browser (no page refresh)
3. **View changes** - Open http://localhost:3000

### Working with Both Backend Versions

The frontend includes a backend selector that lets you test both instrumentation approaches:

1. **Open frontend**: http://localhost:3000
2. **Click "Spring Boot Starter (Manual)"** - Routes to port 3010
3. **Click "OTEL Java Agent (Automatic)"** - Routes to port 3011
4. **Make requests** - See how both backends behave

## OpenTelemetry Development (Backend Agent)

### Viewing Traces While Developing

One of the best parts: you can **see your changes in Honeycomb immediately**!

1. **Open Honeycomb UI**: https://ui.honeycomb.io
2. **Make a code change** in `backends/springboot-agent/`
3. **Save the file** - Wait 2-5 seconds for reload
4. **Trigger a request** via frontend at http://localhost:3000
5. **Refresh Honeycomb** - See your new traces with changes

### Example: Adding Custom Spans

**Without hot reload, you'd have to:**
1. Edit code
2. Stop containers
3. Rebuild images
4. Restart containers
5. Test

**With hot reload:**
1. Edit code
2. Save
3. Test (2-5 seconds later)

### Adding Custom Attributes

```java
import io.opentelemetry.api.trace.Span;

@GetMapping("/api/frontend_to_backend")
public Map<String, Object> handleGet() {
    // Add custom attribute to current span
    Span currentSpan = Span.current();
    currentSpan.setAttribute("custom.attribute", "test-value");

    // Your logic here...
}
```

Save, wait 3 seconds, test, and see it in Honeycomb!

## Services & Access Points

| Service | Port | URL | Purpose |
|---------|------|-----|---------|
| Frontend | 3000 | http://localhost:3000 | React UI with backend selector |
| Upstream | 3002 | http://localhost:3002/api/backend_to_upstream | Data service |
| Backend Starter | 3010 | http://localhost:3010/api/frontend_to_backend | Spring Boot Starter (manual) |
| Backend Agent | 3011 | http://localhost:3011/api/frontend_to_backend | OTEL Java Agent (automatic) |

### Health Checks
```bash
curl http://localhost:3010/actuator/health  # Backend starter
curl http://localhost:3011/actuator/health  # Backend agent
curl http://localhost:3002/actuator/health  # Upstream
```

## Development Workflow

### Typical iteration:

```bash
# 1. Start services (first time is slow)
docker-compose up

# 2. Open frontend
open http://localhost:3000

# 3. Edit code in your IDE
# - backends/sprintboot-starter/src/...
# - backends/springboot-agent/src/...
# - upstream/src/...
# - frontend/src/...

# 4. Save file and wait for reload
# - Java: 2-5 seconds
# - React: instant

# 5. Test via frontend or curl
curl http://localhost:3011/api/frontend_to_backend

# 6. View traces in Honeycomb (agent version only)
open https://ui.honeycomb.io

# 7. Repeat steps 3-6 as needed
```

## What Gets Reloaded?

### ✅ Auto-reloads:
- Java source files (`.java`) - **including instrumentation**
- React/TypeScript files (`.tsx`, `.ts`, `.jsx`, `.js`)
- CSS files
- HTML files

### ⚠️ Requires manual restart:
- Configuration in `application.properties`
  ```bash
  docker-compose restart backend-agent
  ```

### ❌ Requires rebuild:
- `pom.xml` dependency changes
  ```bash
  docker-compose build backend-agent
  ```
- `package.json` dependency changes
  ```bash
  docker-compose build frontend
  ```
- OpenTelemetry agent JAR updates
- Dockerfile changes

## Performance Tips

### Maven Cache
The dev setup uses Docker volumes to cache Maven dependencies:
- `maven-repo-upstream` - Upstream's Maven cache
- `maven-repo-backend-starter` - Starter backend's Maven cache
- `maven-repo-backend-agent` - Agent backend's Maven cache

**First build**: Slow (~2-3 minutes) - downloads all dependencies
**Subsequent builds**: Fast (~10-20 seconds) - uses cached dependencies

### Expected Reload Times
- **First compile after start**: 20-30 seconds
- **Subsequent Java reloads**: 2-5 seconds
- **React HMR**: < 1 second

### Viewing Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend-agent
docker-compose logs -f backend-starter
docker-compose logs -f upstream
docker-compose logs -f frontend

# Filter for reload events
docker-compose logs -f backend-agent | grep -i restart

# Filter for OpenTelemetry messages
docker-compose logs backend-agent | grep -i otel
```

## Stopping Services

```bash
# Stop and remove containers
docker-compose down

# Stop, remove containers, and clean volumes (fresh start)
docker-compose down -v
```

## Troubleshooting

### Hot reload not working?

**Check if DevTools is running:**
```bash
docker-compose logs backend-agent | grep -i devtools
```
You should see: `LiveReload server is running on port 35729`

**Verify volume mounts:**
```bash
docker inspect demo-backend-agent | grep -A 10 Mounts
```

**Force a rebuild:**
```bash
docker-compose down
docker-compose build --no-cache
docker-compose up
```

### Changes not appearing?

1. **Check file was saved** - Some editors save to temp files
2. **Check correct file** - Make sure you're editing the mounted directory
3. **Check logs** - Look for compilation errors:
   ```bash
   docker-compose logs -f backend-agent
   ```
4. **Manual restart** - If stuck:
   ```bash
   docker-compose restart backend-agent
   ```

### OpenTelemetry agent not working?

**Check agent is loaded:**
```bash
docker-compose logs backend-agent | grep -i "opentelemetry"
```
You should see agent initialization messages.

**Verify traces in Honeycomb:**
1. Open https://ui.honeycomb.io
2. Query for traces from `backend` service
3. If no traces appear, check:
   - Service logs for OTLP export messages
   - Your Honeycomb API key in `.env`
   - Network connectivity from containers

**Verify OTel agent mount:**
```bash
docker inspect demo-backend-agent | grep -A 20 Mounts
```
Should show `/otel` mounted.

### Container won't start?

**Check for port conflicts:**
```bash
lsof -i :3000  # Frontend
lsof -i :3002  # Upstream
lsof -i :3010  # Backend starter
lsof -i :3011  # Backend agent
```

**View detailed logs:**
```bash
docker-compose up --build
```

## Hot Reload Setup

This project uses a development-focused setup with hot reload enabled by default:

- **Frontend**: Vite dev server with instant HMR
- **Backend Services**: Spring Boot DevTools with 2-5 second reload
- **Maven Cache**: Persistent volumes for faster rebuilds
- **Source Mounting**: Changes on host immediately available in containers

First startup is slower (~2-3 min) as Maven downloads dependencies, but subsequent starts are much faster with cached dependencies.

## Additional Commands

```bash
# Rebuild specific service
docker-compose build backend-agent

# Restart specific service
docker-compose restart backend-agent

# View running containers
docker-compose ps

# Execute command in running container
docker-compose exec backend-agent bash

# Clean everything (nuclear option)
docker-compose down -v
docker system prune -a
```

## Directory Structure Reference

```
otel-sprintboot/
├── backends/
│   ├── sprintboot-starter/       # Spring Boot Starter (manual instrumentation)
│   │   └── src/main/java/...
│   └── springboot-agent/         # OTEL Java Agent (automatic instrumentation)
│       └── src/main/java/...
├── upstream/
│   └── src/main/java/...         # Upstream data service
├── frontend/
│   └── src/                      # React frontend
└── docker-compose.yml            # Main compose file with hot reload
```
