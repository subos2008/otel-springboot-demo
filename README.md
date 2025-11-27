# OpenTelemetry Spring Boot Demo

Multi-version Spring Boot microservices demonstrating OpenTelemetry instrumentation patterns.

## Structure

```
otel-sprintboot/
├── frontend/              # React UI with backend selector (port 3000)
├── upstream/              # Spring Boot data service (port 3002)
├── backends/
│   ├── sprintboot-starter/   # Spring Boot Starter instrumentation (port 3010)
│   └── springboot-agent/     # OTEL Java Agent instrumentation (port 3011)
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

Frontend includes backend selector to toggle between two OpenTelemetry instrumentation approaches.

## Services

| Service | Port | Description |
|---------|------|-------------|
| Frontend | 3000 | React UI with backend selector |
| Upstream | 3002 | Data service (shared) - echos data back to the backend |
| Backend Starter | 3010 | Spring Boot Starter (manual instrumentation) |
| Backend Agent | 3011 | OTEL Java Agent (automatic instrumentation) |

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

**Starter** - OpenTelemetry Spring Boot Starter (manual instrumentation):
- Code-based instrumentation via Spring libraries
- @WithSpan annotations for custom spans
- RestTemplateBuilder for context propagation
- Manual span manipulation with Span API
- OTLP export to Honeycomb

**Agent** - OpenTelemetry Java Agent (automatic instrumentation):
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
- OpenTelemetry Java Agent & Spring Boot Starter
- Apache Camel 3.11.5
- Docker Compose

## Documentation

- [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) - Hot reload workflow
- [docs/OTEL_SETUP.md](docs/OTEL_SETUP.md) - OpenTelemetry configuration
- [docs/implementation-plan.md](docs/implementation-plan.md) - Technical specs
- [CLAUDE.md](CLAUDE.md) - AI assistant guide
