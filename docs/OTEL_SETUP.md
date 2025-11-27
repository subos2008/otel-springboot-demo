# OpenTelemetry Setup with .env

This project uses a centralized `.env` file at the repository root to configure OpenTelemetry settings for all services.

## How It Works

### File Structure
```
otel-sprintboot/
├── .env                          # Root configuration (Honeycomb settings)
├── springboot-agent/
│   ├── .env -> ../.env          # Symlink to root .env
│   ├── docker-compose.yml
│   └── docker-compose.dev.yml
└── sprintboot-starter/
    └── .env -> ../.env          # Symlink to root .env
```

### Symlinks
The subdirectories use **symlinks** to the root `.env` file:
- ✅ **Single source of truth** - Edit once, applies everywhere
- ✅ **No duplication** - Changes automatically propagate
- ✅ **Git-friendly** - Symlinks are version-controlled

## Current Configuration

The `.env` file is configured for **Honeycomb**:

```bash
# OpenTelemetry Configuration for Honeycomb
OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
OTEL_EXPORTER_OTLP_ENDPOINT=https://api.honeycomb.io
OTEL_EXPORTER_OTLP_HEADERS=x-honeycomb-team=YOUR_API_KEY

# Telemetry settings
OTEL_TRACES_EXPORTER=otlp
OTEL_METRICS_EXPORTER=otlp
OTEL_LOGS_EXPORTER=none

# Resource attributes (applies to all services)
OTEL_RESOURCE_ATTRIBUTES=deployment.environment=local
```

Each service sets its own `OTEL_SERVICE_NAME` in docker-compose files:
- **Backend**: `OTEL_SERVICE_NAME=backend`
- **Upstream**: `OTEL_SERVICE_NAME=upstream`

## Verify Configuration

Check that Docker Compose reads the `.env` file correctly:

```bash
cd springboot-agent
docker-compose config | grep OTEL_EXPORTER

# Expected output:
# OTEL_EXPORTER_OTLP_ENDPOINT: https://api.honeycomb.io
# OTEL_EXPORTER_OTLP_HEADERS: x-honeycomb-team=...
```

## Switching Observability Backends

### Using Honeycomb (Current)
Already configured! Just run:
```bash
cd springboot-agent
docker-compose up
```

Traces appear in: https://ui.honeycomb.io

### Using Local Jaeger
To test locally with Jaeger:

1. **Update .env**:
   ```bash
   OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger:4318
   # OTEL_EXPORTER_OTLP_HEADERS=x-honeycomb-team=...  # Comment out
   ```

2. **Add Jaeger service** to docker-compose.yml:
   ```yaml
   jaeger:
     image: jaegertracing/all-in-one:latest
     ports:
       - "16686:16686"
       - "4318:4318"
   ```

3. **Restart**:
   ```bash
   docker-compose down && docker-compose up
   ```

4. **View traces**: http://localhost:16686

### Using Other Backends

Update `.env` with your backend's settings:

```bash
# Grafana Cloud
OTEL_EXPORTER_OTLP_ENDPOINT=https://otlp-gateway-prod-us-central-0.grafana.net/otlp
OTEL_EXPORTER_OTLP_HEADERS=Authorization=Basic YOUR_BASE64_TOKEN

# New Relic
OTEL_EXPORTER_OTLP_ENDPOINT=https://otlp.nr-data.net:4318
OTEL_EXPORTER_OTLP_HEADERS=api-key=YOUR_NEW_RELIC_KEY

# Lightstep
OTEL_EXPORTER_OTLP_ENDPOINT=https://ingest.lightstep.com:443
OTEL_EXPORTER_OTLP_HEADERS=lightstep-access-token=YOUR_TOKEN
```

## Environment Variables Reference

### Shared Variables (from .env)
| Variable | Description | Example |
|----------|-------------|---------|
| `OTEL_EXPORTER_OTLP_ENDPOINT` | OTLP receiver endpoint | `https://api.honeycomb.io` |
| `OTEL_EXPORTER_OTLP_HEADERS` | Authentication headers | `x-honeycomb-team=...` |
| `OTEL_EXPORTER_OTLP_PROTOCOL` | Protocol format | `http/protobuf` |
| `OTEL_TRACES_EXPORTER` | Trace exporter type | `otlp` |
| `OTEL_METRICS_EXPORTER` | Metrics exporter type | `otlp` |
| `OTEL_LOGS_EXPORTER` | Logs exporter type | `none` |
| `OTEL_RESOURCE_ATTRIBUTES` | Additional resource tags | `deployment.environment=local` |

### Service-Specific Variables (from docker-compose.yml)
| Variable | Description | Set Per Service |
|----------|-------------|-----------------|
| `OTEL_SERVICE_NAME` | Service identifier | `backend`, `upstream` |
| `JAVA_TOOL_OPTIONS` | Java agent path | `-javaagent:/otel/...` |
| `UPSTREAM_SERVICE_URL` | Backend→Upstream URL | `http://upstream:3002` |

## Security Best Practices

⚠️ **The `.env` file contains your API key!**

- ✅ `.env` is in `.gitignore` (never commit)
- ✅ Symlinks are tracked (they don't contain secrets)
- ✅ Use `.env.example` as a template (no secrets)

**Check if .env is ignored:**
```bash
git status --ignored | grep .env
```

**If .env was accidentally committed:**
```bash
git rm --cached .env
git commit -m "Remove .env from version control"
```

**Create template for others:**
```bash
cp .env .env.example
# Edit .env.example and replace API key with placeholder
git add .env.example
git commit -m "Add .env template"
```

## Troubleshooting

### Variables not being picked up?

1. **Check symlinks exist**:
   ```bash
   ls -la springboot-agent/.env
   # Should show: .env -> ../.env
   ```

2. **Verify .env format** (no `export` keyword):
   ```bash
   cat .env
   # Should be: VARIABLE=value
   # NOT: export VARIABLE=value
   ```

3. **Restart containers**:
   ```bash
   docker-compose down && docker-compose up
   ```

### Symlinks not working on Windows?

Windows may require developer mode for symlinks. Alternative:

```bash
# Copy .env instead of symlinking
cp .env springboot-agent/.env
cp .env sprintboot-starter/.env
```

**Note**: You'll need to update multiple files when configuration changes.

### Traces not appearing?

1. **Check API key**:
   ```bash
   grep OTEL_EXPORTER_OTLP_HEADERS .env
   ```

2. **View service logs**:
   ```bash
   docker-compose logs backend | grep -i "opentelemetry"
   ```

3. **Test connectivity**:
   ```bash
   docker-compose exec backend curl -v https://api.honeycomb.io
   ```

## Quick Reference

```bash
# Edit configuration
vim .env

# Verify changes
docker-compose config | grep OTEL

# Apply changes
docker-compose down && docker-compose up

# View traces
# Honeycomb: https://ui.honeycomb.io
# Jaeger: http://localhost:16686
```

## Multiple Environments

Use different .env files for different environments:

```bash
# Create environment-specific files
.env.development
.env.staging
.env.production

# Use specific file:
docker-compose --env-file .env.development up
```
