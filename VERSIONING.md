# App Version Tagging in OpenTelemetry

This document explains how different versions of the application are distinguished in telemetry data using OpenTelemetry resource attributes.

## Problem

We have multiple versions of the same application:
- **sprintboot-starter** - Baseline version with no instrumentation
- **springboot-agent** - Java agent auto-instrumentation version
- *(Future)* **springboot-starter-otel** - Manual instrumentation version

When viewing traces in Honeycomb (or any observability backend), we need to distinguish which version generated each trace.

## Solution: Resource Attributes

We use **OTEL_RESOURCE_ATTRIBUTES** to tag each service with metadata about its version and instrumentation type.

### What are Resource Attributes?

Resource attributes are key-value pairs attached to **all telemetry data** (traces, metrics, logs) from a service. They describe the service itself, not individual requests.

Think of them as "tags" that identify:
- What version of the app is this?
- How is it instrumented?
- What environment is it running in?

## Current Configuration

### springboot-agent

```yaml
OTEL_RESOURCE_ATTRIBUTES=app.version=springboot-agent,instrumentation.type=java-agent,deployment.environment=local
```

**Attributes:**
- `app.version=springboot-agent` - Identifies this as the Java agent version
- `instrumentation.type=java-agent` - Shows it uses auto-instrumentation
- `deployment.environment=local` - Indicates it's running locally (from .env)

### sprintboot-starter (when instrumented)

When we add instrumentation to the starter version, it would use:
```yaml
OTEL_RESOURCE_ATTRIBUTES=app.version=sprintboot-starter,instrumentation.type=none,deployment.environment=local
```

### Future: springboot-starter-otel

```yaml
OTEL_RESOURCE_ATTRIBUTES=app.version=springboot-starter-otel,instrumentation.type=manual,deployment.environment=local
```

## How It Works

### 1. Base Configuration (.env)

The root `.env` file sets a default:
```bash
OTEL_RESOURCE_ATTRIBUTES=deployment.environment=local
```

### 2. Version-Specific Override (docker-compose.yml)

Each docker-compose file **appends** to this base:
```yaml
environment:
  - OTEL_RESOURCE_ATTRIBUTES=app.version=springboot-agent,instrumentation.type=java-agent,${OTEL_RESOURCE_ATTRIBUTES}
```

**Result:** The attributes are combined:
```
app.version=springboot-agent,instrumentation.type=java-agent,deployment.environment=local
```

### 3. Development Override (docker-compose.dev.yml)

The dev version adds an extra attribute:
```yaml
environment:
  - OTEL_RESOURCE_ATTRIBUTES=app.version=springboot-agent,instrumentation.type=java-agent,deployment.environment=development,${OTEL_RESOURCE_ATTRIBUTES}
```

**Note:** `deployment.environment=development` overrides the `deployment.environment=local` from .env.

## Querying in Honeycomb

### Filter by Version

**Show only springboot-agent traces:**
```
app.version = "springboot-agent"
```

**Show only manual instrumentation:**
```
instrumentation.type = "manual"
```

### Compare Versions

**Query:**
```
GROUP BY app.version
```

**Result:** See traces grouped by version, allowing you to compare:
- Performance differences
- Error rates
- Request patterns

### Filter by Environment

**Show only production:**
```
deployment.environment = "production"
```

**Show dev vs local:**
```
WHERE deployment.environment IN ["development", "local"]
GROUP BY deployment.environment
```

## Attribute Naming Conventions

We follow **OpenTelemetry semantic conventions** where possible:

### Standard Attributes (from OTel spec)
- `service.name` - Set via `OTEL_SERVICE_NAME` (backend, upstream)
- `deployment.environment` - Environment (local, development, staging, production)

### Custom Attributes (our additions)
- `app.version` - Application version identifier
- `instrumentation.type` - How telemetry is generated

**Why custom attributes?**
OpenTelemetry doesn't have standard attributes for "app version" in the context of multiple instrumentation approaches, so we use namespaced custom attributes.

## Adding New Versions

When you create a new version (e.g., `springboot-starter-otel`):

### 1. Create the compose file

```yaml
# springboot-starter-otel/docker-compose.yml
services:
  backend:
    environment:
      - OTEL_SERVICE_NAME=backend
      - OTEL_RESOURCE_ATTRIBUTES=app.version=springboot-starter-otel,instrumentation.type=manual,${OTEL_RESOURCE_ATTRIBUTES}
      # ... other OTEL vars

  upstream:
    environment:
      - OTEL_SERVICE_NAME=upstream
      - OTEL_RESOURCE_ATTRIBUTES=app.version=springboot-starter-otel,instrumentation.type=manual,${OTEL_RESOURCE_ATTRIBUTES}
      # ... other OTEL vars
```

### 2. Create symlink to .env

```bash
cd springboot-starter-otel
ln -s ../.env .env
```

### 3. Run and verify

```bash
docker-compose config | grep OTEL_RESOURCE_ATTRIBUTES
```

Should show:
```
OTEL_RESOURCE_ATTRIBUTES: app.version=springboot-starter-otel,instrumentation.type=manual,deployment.environment=local
```

## Viewing Attributes in Traces

### In Honeycomb

1. **Open a trace** in Honeycomb UI
2. **Expand the root span**
3. **Look for Resource Attributes section**

You'll see:
```
service.name: backend
app.version: springboot-agent
instrumentation.type: java-agent
deployment.environment: local
```

### In Jaeger (Local)

1. **Open http://localhost:16686**
2. **Select a trace**
3. **Look at the Process section**

Shows:
```json
{
  "serviceName": "backend",
  "tags": {
    "app.version": "springboot-agent",
    "instrumentation.type": "java-agent",
    "deployment.environment": "local"
  }
}
```

## Common Queries

### Compare all versions side-by-side

**Honeycomb Query:**
```
CALCULATE COUNT
GROUP BY app.version
VISUALIZE AS Heatmap
```

### Find slowest version

**Honeycomb Query:**
```
CALCULATE P95(duration_ms)
GROUP BY app.version
ORDER BY P95(duration_ms) DESC
```

### Show traces from specific instrumentation

**Filter:**
```
instrumentation.type = "java-agent"
```

### Production traces only

**Filter:**
```
deployment.environment = "production"
AND app.version = "springboot-agent"
```

## Best Practices

### 1. Keep Version Names Consistent
✅ Good: `springboot-agent`, `springboot-starter-otel`
❌ Bad: `agent-version`, `v2-otel`, `new-app`

### 2. Use Semantic Instrumentation Types
- `none` - No instrumentation
- `java-agent` - Auto-instrumentation via Java agent
- `manual` - Manual instrumentation via SDK
- `hybrid` - Mix of auto and manual

### 3. Set Environment Appropriately
- `local` - Developer laptop
- `development` - Dev server/cluster
- `staging` - Pre-production
- `production` - Live production

### 4. Don't Overuse Attributes
Too many attributes = harder to query. Stick to:
- Version identifier
- Instrumentation type
- Environment
- (Maybe) deployment region if multi-region

### 5. Document Custom Attributes
Keep this file updated when you add new attributes!

## Environment-Specific Configuration

### Local Development
```bash
# .env
OTEL_RESOURCE_ATTRIBUTES=deployment.environment=local
```

### Staging
```bash
# .env.staging
OTEL_RESOURCE_ATTRIBUTES=deployment.environment=staging
```

### Production
```bash
# .env.production
OTEL_RESOURCE_ATTRIBUTES=deployment.environment=production
```

**Run with specific env:**
```bash
docker-compose --env-file .env.production up
```

## Troubleshooting

### Attributes not showing up in Honeycomb?

1. **Verify configuration:**
   ```bash
   docker-compose config | grep OTEL_RESOURCE_ATTRIBUTES
   ```

2. **Check service logs:**
   ```bash
   docker-compose logs backend | grep -i "resource"
   ```

3. **Look for agent initialization:**
   ```bash
   docker-compose logs backend | grep -i "opentelemetry"
   ```

4. **Test with curl:**
   ```bash
   curl http://localhost:3001/api/frontend_to_backend
   ```
   Then check Honeycomb for the trace.

### Attributes have wrong values?

**Common issues:**
- **Missing comma** between attributes: `app.version=test instrumentation.type=agent` ❌
- **Should be:** `app.version=test,instrumentation.type=agent` ✅

- **Quotes in values** (don't use quotes): `app.version="test"` ❌
- **Should be:** `app.version=test` ✅

### Duplicate attributes?

If you see `deployment.environment` twice, check:
1. Is it set in both `.env` and docker-compose?
2. Last value wins, but avoid duplication

**Fix:** Remove from `.env` or use a different name.

## Summary

**Key Points:**
- ✅ Use `OTEL_RESOURCE_ATTRIBUTES` to tag versions
- ✅ Set `app.version` to distinguish app versions
- ✅ Set `instrumentation.type` to show how it's instrumented
- ✅ Use `.env` for shared attributes, docker-compose for version-specific
- ✅ Query by these attributes in Honeycomb to compare versions

**Attribute Format:**
```
key1=value1,key2=value2,key3=value3
```

**Current Versions:**
| Version | app.version | instrumentation.type |
|---------|-------------|---------------------|
| springboot-agent | `springboot-agent` | `java-agent` |
| *(future)* sprintboot-starter | `sprintboot-starter` | `none` |
| *(future)* springboot-starter-otel | `springboot-starter-otel` | `manual` |
