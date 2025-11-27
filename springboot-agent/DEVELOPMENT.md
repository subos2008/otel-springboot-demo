# Development Guide - Hot Reload with Docker (OpenTelemetry Agent Version)

This guide explains how to use Docker with hot reload enabled for the OpenTelemetry Java Agent version.

## Quick Start

```bash
# Start with hot reload enabled
docker-compose -f docker-compose.dev.yml up --build

# Or run in detached mode
docker-compose -f docker-compose.dev.yml up -d --build
```

## What's Different from the Starter Version?

This version includes:
- ✅ All hot reload features from the starter version
- ✅ **OpenTelemetry Java Agent** for automatic instrumentation
- ✅ **Honeycomb integration** for trace visualization
- ✅ Hot reload works with the agent attached!

## How Hot Reload Works

### Spring Boot Services (Backend & Upstream)
- **Technology**: Spring Boot DevTools + Maven + OpenTelemetry Agent
- **What's mounted**: Source code (`src/`), `pom.xml`, and OTel agent JAR
- **Reload trigger**: When you save a `.java` file
- **Reload time**: ~2-5 seconds (Maven recompiles and Spring Boot restarts)
- **Agent behavior**: Automatically re-instruments on restart

### Frontend (React)
- **Technology**: Vite HMR (Hot Module Replacement)
- **What's mounted**: Source code (`src/`), `index.html`, `vite.config.ts`
- **Reload trigger**: When you save any source file
- **Reload time**: Instant (<1 second)

## Viewing Traces While Developing

One of the best parts of this setup: you can **see your changes in Honeycomb immediately**!

1. **Open Honeycomb UI**: https://ui.honeycomb.io
2. **Make a code change** (e.g., add a log statement, change a response)
3. **Save the file** - Wait 2-5 seconds for reload
4. **Trigger a request**: http://localhost:3000
5. **Refresh Honeycomb** - See your new traces with changes

### Example: Adding Custom Spans

**Before hot reload, you'd have to:**
1. Edit code
2. Stop containers
3. Rebuild images
4. Restart containers
5. Test

**With hot reload:**
1. Edit code
2. Save
3. Test (2-5 seconds later)

## Making Changes

### Edit Java Code with Instrumentation

1. **Edit a controller** - Example: `backend/src/main/java/com/demo/backend/BackendController.java`
   ```java
   @GetMapping("/api/frontend_to_backend")
   public Map<String, Object> handleGet() {
       // Add a log to see in traces
       logger.info("Processing GET request - with hot reload!");

       // Your changes here
       ...
   }
   ```

2. **Save the file** - Watch the logs:
   ```bash
   docker-compose -f docker-compose.dev.yml logs -f backend
   ```

3. **Test and trace**:
   ```bash
   # Make a request
   curl http://localhost:3001/api/frontend_to_backend

   # View in Honeycomb
   open https://ui.honeycomb.io
   ```

4. **See instrumentation** - The OpenTelemetry agent automatically:
   - Captures HTTP spans
   - Propagates context between services
   - Reports to Jaeger
   - No manual instrumentation needed!

## Services

### Access Points
- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:3001
- **Upstream API**: http://localhost:3002
- **Honeycomb UI**: https://ui.honeycomb.io

### Check Service Health
```bash
curl http://localhost:3001/actuator/health
curl http://localhost:3002/actuator/health
```

## Development Workflow

### Typical iteration:

```bash
# 1. Start services (first time is slow)
docker-compose -f docker-compose.dev.yml up

# 2. Edit code in your IDE
# - backend/src/main/java/com/demo/backend/BackendController.java
# - upstream/src/main/java/com/demo/upstream/UpstreamController.java

# 3. Save file

# 4. Wait 2-5 seconds, then test
curl http://localhost:3001/api/frontend_to_backend

# 5. View traces in Honeycomb
open https://ui.honeycomb.io

# 6. Repeat steps 2-5 as needed
```

## OpenTelemetry Configuration

The agent is configured via environment variables in `docker-compose.dev.yml`:

```yaml
environment:
  - JAVA_TOOL_OPTIONS=-javaagent:/otel/opentelemetry-javaagent.jar
  - OTEL_SERVICE_NAME=backend  # or upstream
  - OTEL_TRACES_EXPORTER=otlp
  - OTEL_METRICS_EXPORTER=otlp
  - OTEL_LOGS_EXPORTER=none
  - OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger:4318
  - OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
  - SPRING_DEVTOOLS_RESTART_ENABLED=true
```

**Note**: The agent persists across hot reloads! You don't need to restart the container to see instrumentation changes.

## What Gets Reloaded?

### ✅ Auto-reloads:
- Java source files (`.java`) - **including instrumentation**
- React/TypeScript files (`.tsx`, `.ts`, `.jsx`, `.js`)
- CSS files
- HTML files
- Configuration in `application.properties` (requires manual restart)

### ❌ Requires rebuild:
- `pom.xml` dependency changes
- OpenTelemetry agent JAR updates
- Dockerfile changes

## Performance Tips

### Maven Cache
The dev setup uses Docker volumes to cache Maven dependencies:
- First build: Slow (~2-3 minutes)
- Subsequent reloads: Fast (~2-5 seconds)

### Honeycomb Performance
With hot reload and frequent testing, you'll generate many traces in Honeycomb. Use Honeycomb's filtering to focus on recent traces or specific queries.

### Viewing Logs

```bash
# All services
docker-compose -f docker-compose.dev.yml logs -f

# Specific service
docker-compose -f docker-compose.dev.yml logs -f backend
docker-compose -f docker-compose.dev.yml logs -f upstream

# Filter for OpenTelemetry agent messages
docker-compose -f docker-compose.dev.yml logs backend | grep -i otel

# Filter for reload events
docker-compose -f docker-compose.dev.yml logs backend | grep -i restart
```

## Stopping Services

```bash
# Stop and remove containers
docker-compose -f docker-compose.dev.yml down

# Stop, remove containers, and clean volumes (fresh start)
docker-compose -f docker-compose.dev.yml down -v
```

## Troubleshooting

### OpenTelemetry agent not working?

**Check agent is loaded:**
```bash
docker-compose -f docker-compose.dev.yml logs backend | grep -i "opentelemetry"
```
You should see agent initialization messages.

**Verify traces in Honeycomb:**
1. Open https://ui.honeycomb.io
2. Select your environment/dataset
3. Query for traces from `backend` or `upstream` services
4. If no traces appear, check:
   - Service logs for OTLP export messages
   - Your Honeycomb API key in `.env`
   - Network connectivity from containers

### Hot reload not working?

**Check DevTools:**
```bash
docker-compose -f docker-compose.dev.yml logs backend | grep -i devtools
```

**Verify OTel agent mount:**
```bash
docker inspect demo-backend-agent-dev | grep -A 20 Mounts
```
Should show `/otel` mounted.

**Force rebuild:**
```bash
docker-compose -f docker-compose.dev.yml down
docker-compose -f docker-compose.dev.yml build --no-cache
docker-compose -f docker-compose.dev.yml up
```

### Agent conflicts with hot reload?

This is rare, but if the agent interferes with DevTools:

1. **Check agent version** - Ensure you have a recent OTel agent
2. **Check logs** - Look for agent errors during restart
3. **Disable agent temporarily**:
   - Comment out `JAVA_TOOL_OPTIONS` in `docker-compose.dev.yml`
   - Restart containers

## Production vs Development

### Use `docker-compose.yml` (production) when:
- Running demos
- Testing the full instrumented stack
- No code changes needed
- Want optimized Docker images

### Use `docker-compose.dev.yml` (development) when:
- Actively developing instrumentation
- Making frequent code changes
- Testing different span configurations
- Want hot reload

## Comparison

| Feature | docker-compose.yml | docker-compose.dev.yml |
|---------|-------------------|------------------------|
| **Startup time** | Fast (~30 sec) | Slow (~2-3 min first time) |
| **Hot reload** | ❌ No | ✅ Yes |
| **OpenTelemetry** | ✅ Yes | ✅ Yes |
| **Honeycomb** | ✅ Yes | ✅ Yes |
| **Code changes** | Requires rebuild | Auto-reloads |
| **Image size** | Optimized | Large (includes Maven) |
| **Use case** | Production-like | Development |

## Additional Commands

```bash
# Rebuild specific service
docker-compose -f docker-compose.dev.yml build backend

# Restart specific service
docker-compose -f docker-compose.dev.yml restart backend

# View running containers
docker-compose -f docker-compose.dev.yml ps

# Execute command in running container
docker-compose -f docker-compose.dev.yml exec backend bash

# View traces in Honeycomb
# https://ui.honeycomb.io → Query for your services

# Clean everything
docker-compose -f docker-compose.dev.yml down -v
```

## Tips for OpenTelemetry Development

### Experimenting with Instrumentation

1. **Add custom attributes to spans** - Edit controller methods
2. **Test context propagation** - Make changes to service communication
3. **View immediately in Jaeger** - No rebuild needed!

### Example: Adding a custom attribute

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
