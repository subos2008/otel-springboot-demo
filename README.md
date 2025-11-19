# OpenTelemetry Spring Boot Demo

This repository contains multiple versions of a Spring Boot microservices demo application, designed to demonstrate observability patterns and OpenTelemetry instrumentation.

## Project Versions

### 1. sprintboot-starter
The base implementation with three microservices:
- React frontend (port 3000)
- Spring Boot backend service (port 3001)
- Spring Boot upstream service (port 3002)

This version demonstrates basic HTTP communication patterns between services without any observability instrumentation.

**Features:**
- HTTP method testing (GET, POST, PUT, DELETE)
- Continuous request mode
- Health monitoring dashboard
- Request history tracking

👉 **[Get Started with sprintboot-starter](./sprintboot-starter/README.md)**

### Future Versions
Additional versions will be added to demonstrate:
- OpenTelemetry auto-instrumentation
- Custom instrumentation
- Distributed tracing
- Metrics collection
- Advanced observability patterns

## Quick Start

```bash
# Navigate to the starter version
cd sprintboot-starter

# Start all services with Docker
docker-compose up --build

# Access the frontend
open http://localhost:3000
```

## Repository Structure

```
otel-sprintboot/
├── README.md                           # This file
├── docs/
│   ├── project-genesis-prompt.md       # Original project specification
│   └── implementation-plan.md          # Detailed implementation plan
└── sprintboot-starter/                 # Version 1: Basic implementation
    ├── README.md                       # Detailed documentation
    ├── docker-compose.yml
    ├── frontend/                       # React SPA
    ├── backend/                        # Spring Boot proxy service
    └── upstream/                       # Spring Boot timestamp service
```

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                      Frontend (React)                    │
│                       Port 3000                          │
└────────────────┬────────────────────────────────────────┘
                 │ HTTP Requests
                 ▼
┌─────────────────────────────────────────────────────────┐
│                  Backend Service (Spring Boot)           │
│                       Port 3001                          │
│  - Receives requests from frontend                       │
│  - Transforms and forwards to upstream                   │
│  - Aggregates health information                         │
└────────────────┬────────────────────────────────────────┘
                 │ HTTP Requests
                 ▼
┌─────────────────────────────────────────────────────────┐
│                 Upstream Service (Spring Boot)           │
│                       Port 3002                          │
│  - Returns timestamp data                                │
│  - Simple REST endpoints                                 │
└─────────────────────────────────────────────────────────┘
```

## Prerequisites

- **Docker & Docker Compose** (recommended)
  - Docker 20.10+
  - Docker Compose 2.0+

- **Local Development**
  - Java 18+
  - Maven 3.6+
  - Node.js 18+

## Documentation

- [Project Genesis Prompt](./docs/project-genesis-prompt.md) - Original project requirements
- [Implementation Plan](./docs/implementation-plan.md) - Detailed technical specifications
- [Starter Version README](./sprintboot-starter/README.md) - Setup and usage instructions

## Contributing

This is a demo project for learning and experimentation. Feel free to:
- Explore the code
- Modify the services
- Add new features
- Create new versions with different instrumentation approaches

## License

Educational and demonstration purposes.
