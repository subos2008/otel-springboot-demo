# Development Guide - Hot Reload with Docker

This guide explains how to use Docker with hot reload enabled for faster development.

## Quick Start

```bash
# Start with hot reload enabled
docker-compose -f docker-compose.dev.yml up --build

# Or run in detached mode
docker-compose -f docker-compose.dev.yml up -d --build
```

## How Hot Reload Works

### Spring Boot Services (Backend & Upstream)
- **Technology**: Spring Boot DevTools + Maven
- **What's mounted**: Source code (`src/`) and `pom.xml`
- **Reload trigger**: When you save a `.java` file
- **Reload time**: ~2-5 seconds (Maven recompiles and Spring Boot restarts)

### Frontend (React)
- **Technology**: Vite HMR (Hot Module Replacement)
- **What's mounted**: Source code (`src/`), `index.html`, `vite.config.ts`
- **Reload trigger**: When you save any source file
- **Reload time**: Instant (<1 second)

## Making Changes

### Edit Java Code (Backend/Upstream)

1. **Edit a file** - Example: `backend/src/main/java/com/demo/backend/BackendController.java`
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
   docker-compose -f docker-compose.dev.yml logs -f backend
   ```
   You'll see: `Restarting due to 1 class path change...`

4. **Test the change**:
   ```bash
   curl http://localhost:3001/api/frontend_to_backend
   ```

### Edit React Code (Frontend)

1. **Edit a file** - Example: `frontend/src/App.tsx`
2. **Save the file** - Vite HMR updates instantly in the browser (no page refresh)
3. **View changes** - Open http://localhost:3000

## What Gets Reloaded?

### ✅ Auto-reloads:
- Java source files (`.java`)
- React/TypeScript files (`.tsx`, `.ts`, `.jsx`, `.js`)
- CSS files
- HTML files
- Configuration in `application.properties` (requires manual restart)

### ❌ Requires rebuild:
- `pom.xml` dependency changes (run: `docker-compose -f docker-compose.dev.yml build backend`)
- `package.json` dependency changes (run: `docker-compose -f docker-compose.dev.yml build frontend`)
- Dockerfile changes

## Performance Tips

### Maven Cache
The dev setup uses Docker volumes to cache Maven dependencies:
- `maven-repo-upstream` - Upstream's Maven cache
- `maven-repo-backend` - Backend's Maven cache

**First build**: Slow (~2-3 minutes) - downloads all dependencies
**Subsequent builds**: Fast (~10-20 seconds) - uses cached dependencies

### Viewing Logs

```bash
# All services
docker-compose -f docker-compose.dev.yml logs -f

# Specific service
docker-compose -f docker-compose.dev.yml logs -f backend
docker-compose -f docker-compose.dev.yml logs -f upstream
docker-compose -f docker-compose.dev.yml logs -f frontend

# Filter for reload events
docker-compose -f docker-compose.dev.yml logs -f backend | grep -i restart
```

## Stopping Services

```bash
# Stop and remove containers
docker-compose -f docker-compose.dev.yml down

# Stop, remove containers, and clean volumes (fresh start)
docker-compose -f docker-compose.dev.yml down -v
```

## Troubleshooting

### Hot reload not working?

**Check if DevTools is running:**
```bash
docker-compose -f docker-compose.dev.yml logs backend | grep -i devtools
```
You should see: `LiveReload server is running on port 35729`

**Verify volume mounts:**
```bash
docker inspect demo-backend-dev | grep -A 10 Mounts
```

**Force a rebuild:**
```bash
docker-compose -f docker-compose.dev.yml down
docker-compose -f docker-compose.dev.yml build --no-cache
docker-compose -f docker-compose.dev.yml up
```

### Changes not appearing?

1. **Check file was saved** - Some editors save to temp files
2. **Check correct file** - Make sure you're editing the mounted directory
3. **Check logs** - Look for compilation errors:
   ```bash
   docker-compose -f docker-compose.dev.yml logs -f backend
   ```
4. **Manual restart** - If stuck:
   ```bash
   docker-compose -f docker-compose.dev.yml restart backend
   ```

### Slow compilation?

The first compilation after container start is slow. Subsequent reloads are faster because:
- Maven dependencies are cached
- Only changed classes are recompiled
- Spring Boot DevTools uses classloader tricks for fast restarts

**Expected reload times:**
- First compile: 20-30 seconds
- Subsequent reloads: 2-5 seconds

### Container won't start?

**Check for port conflicts:**
```bash
lsof -i :3001  # Backend
lsof -i :3002  # Upstream
lsof -i :3000  # Frontend
```

**View detailed logs:**
```bash
docker-compose -f docker-compose.dev.yml up --build
```

## Production vs Development

### Use `docker-compose.yml` (production) when:
- Running demos
- Testing the full stack as users will see it
- No code changes needed
- Want faster startup time

### Use `docker-compose.dev.yml` (development) when:
- Actively developing
- Making frequent code changes
- Testing iterations quickly
- Want hot reload

## Comparison

| Feature | docker-compose.yml | docker-compose.dev.yml |
|---------|-------------------|------------------------|
| **Startup time** | Fast (~30 sec) | Slow (~2-3 min first time) |
| **Hot reload** | ❌ No | ✅ Yes |
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

# Clean everything (nuclear option)
docker-compose -f docker-compose.dev.yml down -v
docker system prune -a
```
