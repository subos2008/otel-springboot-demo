# OpenTelemetry Spring Boot Demo

Multi-version Spring Boot microservices demonstrating OpenTelemetry instrumentation patterns.

**Focus:** This project demonstrates tracing for backend services - specifically how to instrument and observe HTTP requests flowing **into** and **out of** backend services. The upstream service is a simple echo service that exists solely to enable tracing of outbound calls from the backends.

## Structure

```
otel-sprintboot/
├── frontend/              # React UI with backend selector (port 3000)
├── upstream/              # Spring Boot data service (port 3002)
├── backends/
│   ├── springboot-starter/    # Spring Boot Starter instrumentation
│   │   ├── rest-app/          # Standard REST (port 3010)
│   │   ├── camel-rest-app/    # Apache Camel routing (port 3012)
│   │   └── camel-rest-app-dev/    # Apache Camel routing - dev copy (port 3013)
│   └── otel-java-agent/       # OTEL Java Agent instrumentation
│       ├── rest-app/          # Standard REST (port 3011)
│       └── camel-rest-app/    # Apache Camel routing (port 3014)
├── otel/                  # OpenTelemetry Java agent JAR
└── docs/                  # Documentation
```

## Quick Start

```bash
# Start all services with hot reload
docker-compose up --build

# Or run in detached mode
docker-compose up -d --build

# Access
open http://localhost:3000
```

Frontend includes backend selector to toggle between different OpenTelemetry instrumentation approaches.

## Services

| Service | Port | Description |
|---------|------|-------------|
| Frontend | 3000 | React UI with backend selector |
| Upstream | 3002 | Data service (shared) - echos data back to the backend |
| Backend Starter REST | 3010 | Spring Boot Starter (manual instrumentation) |
| Backend Agent REST | 3011 | OTEL Java Agent (automatic instrumentation) |
| Backend Camel REST | 3012 | Spring Boot Starter with Apache Camel (routing patterns) |
| Backend Camel REST (DEV) | 3013 | Dev copy of Camel backend for debugging |
| Backend Agent Camel REST | 3014 | OTEL Java Agent with Apache Camel |

## Architecture

```
Frontend (3000)
    │
    ├─→ Backend Starter REST (3010) ──────────→ Upstream (3002)
    ├─→ Backend Agent REST (3011) ────────────→ Upstream (3002)
    ├─→ Backend Camel REST (3012) ────────────→ Upstream (3002)
    ├─→ Backend Camel REST DEV (3013) ────────→ Upstream (3002)
    └─→ Backend Agent Camel REST (3014) ──────→ Upstream (3002)
                │
                └─→ Honeycomb (traces)
```

## Backend Versions

**Starter REST** - OpenTelemetry Spring Boot Starter (manual instrumentation):
- Code-based instrumentation via Spring libraries
- @WithSpan annotations for custom spans
- RestTemplateBuilder for context propagation
- Manual span manipulation with Span API
- OTLP export to Honeycomb

**Agent REST** - OpenTelemetry Java Agent (automatic instrumentation):
- Auto-instrumented HTTP spans
- Context propagation
- OTLP export to Honeycomb
- Zero code changes required

**Camel REST** - Apache Camel with OpenTelemetry Spring Boot Starter:
- Enterprise Integration Patterns via Apache Camel routes
- ProducerTemplate for message-based routing
- Camel HTTP component for HTTP calls
- camel-opentelemetry for automatic route tracing
- Direct endpoints for synchronous request/response
- Per-step span creation for detailed observability

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
