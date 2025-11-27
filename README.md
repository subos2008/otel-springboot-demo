# OpenTelemetry Spring Boot Demo

Multi-version Spring Boot microservices demonstrating OpenTelemetry instrumentation patterns.

## Structure

```
otel-sprintboot/
├── frontend/              # React UI with backend selector (port 3000)
├── upstream/              # Spring Boot data service (port 3002)
├── backends/
│   ├── sprintboot-starter/   # No instrumentation (port 3010)
│   └── springboot-agent/     # OpenTelemetry agent (port 3011)
├── otel/                  # OpenTelemetry Java agent JAR
└── docs/                  # Documentation
```

## Quick Start

```bash
# Production
docker-compose up --build

# Development (hot reload)
docker-compose -f docker-compose.dev.yml up --build

# Access
open http://localhost:3000
```

Frontend includes backend selector to toggle between instrumented/non-instrumented backends.

## Services

| Service | Port | Description |
|---------|------|-------------|
| Frontend | 3000 | React UI with backend selector |
| Upstream | 3002 | Data service (shared) - echos data back to the backend |
| Backend Starter | 3010 | Baseline, no instrumentation |
| Backend Agent | 3011 | OpenTelemetry auto-instrumentation |

## Architecture

```
Frontend (3000)
    │
    ├─→ Backend Starter (3010) ─→ Upstream (3002)
    └─→ Backend Agent (3011) ───→ Upstream (3002)
                │
                └─→ Honeycomb (traces)
```

## Backend Versions

**Starter** - Baseline HTTP microservices without observability.

**Agent** - Automatic OpenTelemetry instrumentation via Java agent:
- Auto-instrumented HTTP spans
- Context propagation
- OTLP export to Honeycomb
- Zero code changes required

## Configuration

OpenTelemetry settings in `.env`:
```bash
OTEL_EXPORTER_OTLP_ENDPOINT=https://api.honeycomb.io
OTEL_EXPORTER_OTLP_HEADERS=x-honeycomb-team=YOUR_API_KEY
OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
```

See [docs/OTEL_SETUP.md](docs/OTEL_SETUP.md) for configuration details.

## Development

Hot reload enabled with Spring DevTools + Vite HMR.

See [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) for development workflow.

## Tech Stack

- Java 17, Spring Boot 2.7.6
- React 18, TypeScript, Vite
- OpenTelemetry Java Agent
- Apache Camel 3.11.5
- Docker Compose

## Documentation

- [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) - Hot reload workflow
- [docs/OTEL_SETUP.md](docs/OTEL_SETUP.md) - OpenTelemetry configuration
- [docs/implementation-plan.md](docs/implementation-plan.md) - Technical specs
- [CLAUDE.md](CLAUDE.md) - AI assistant guide
