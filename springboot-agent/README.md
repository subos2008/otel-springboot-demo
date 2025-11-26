# Spring Boot Demo - Java Agent Instrumentation

This version demonstrates **OpenTelemetry auto-instrumentation** using the Java agent. No code changes are required - instrumentation happens at runtime.

## What's Different from the Starter Version?

- **OpenTelemetry Java Agent** added to both backend and upstream services
- **Jaeger** added for trace visualization
- **Zero code changes** - instrumentation is purely configuration-based
- Environment variables configure telemetry export

## Architecture

```
Frontend (React) → Backend (Spring Boot + Agent) → Upstream (Spring Boot + Agent) → Jaeger
Port 3000           Port 3001                      Port 3002                       Port 16686
```

## Quick Start

```bash
cd springboot-agent
docker-compose up --build
```

**Access the application:**
- Frontend: http://localhost:3000
- Jaeger UI: http://localhost:16686

## How It Works

### Java Agent Auto-Instrumentation

The OpenTelemetry Java agent is attached using the `-javaagent` JVM flag:

```
JAVA_TOOL_OPTIONS=-javaagent:/otel/opentelemetry-javaagent.jar
```

This automatically instruments:
- HTTP servers (Spring MVC)
- HTTP clients (RestTemplate)
- JDBC database calls
- And 100+ other libraries

### Environment Variables

**Backend & Upstream Services:**
```yaml
OTEL_SERVICE_NAME: backend/upstream
OTEL_TRACES_EXPORTER: otlp
OTEL_METRICS_EXPORTER: otlp
OTEL_LOGS_EXPORTER: none
OTEL_EXPORTER_OTLP_ENDPOINT: http://jaeger:4318
OTEL_EXPORTER_OTLP_PROTOCOL: http/protobuf
```

## Viewing Traces

1. Open Jaeger UI: http://localhost:16686
2. Select service: `backend` or `upstream`
3. Click "Find Traces"
4. Click on any trace to see the complete request flow

### What You'll See

- **Service calls** from frontend → backend → upstream
- **HTTP spans** showing request/response details
- **Timing information** for each service
- **Automatic context propagation** between services

## Testing the Instrumentation

### Generate Traces

1. Open http://localhost:3000
2. Click any HTTP method button (GET, POST, PUT, DELETE)
3. Or use continuous mode to generate sustained traffic

### View in Jaeger

1. Go to http://localhost:16686
2. Service dropdown → select "backend"
3. Click "Find Traces"
4. You'll see traces showing:
   - Frontend request to backend
   - Backend request to upstream
   - Response times for each hop

## Running Locally Without Docker

**Terminal 1 - Jaeger:**
```bash
docker run -d --name jaeger \
  -p 16686:16686 \
  -p 4318:4318 \
  jaegertracing/all-in-one:latest
```

**Terminal 2 - Upstream:**
```bash
cd upstream
export JAVA_TOOL_OPTIONS="-javaagent:../otel/opentelemetry-javaagent.jar"
export OTEL_SERVICE_NAME=upstream
export OTEL_TRACES_EXPORTER=otlp
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318
export OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
mvn spring-boot:run
```

**Terminal 3 - Backend:**
```bash
cd backend
export JAVA_TOOL_OPTIONS="-javaagent:../otel/opentelemetry-javaagent.jar"
export OTEL_SERVICE_NAME=backend
export OTEL_TRACES_EXPORTER=otlp
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318
export OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
mvn spring-boot:run
```

**Terminal 4 - Frontend:**
```bash
cd frontend
npm install
npm run dev
```

## Advantages of Java Agent

✅ **No Code Changes:** Works with any Java application
✅ **Comprehensive:** Automatically instruments 100+ libraries
✅ **Quick Setup:** Add agent JAR + environment variables
✅ **Production Ready:** Widely used in production environments
✅ **Framework Agnostic:** Works with Spring, Quarkus, Micronaut, etc.

## Disadvantages

❌ **Less Control:** Can't easily customize what gets instrumented
❌ **Heavier:** Instruments everything, even if you don't need it
❌ **Startup Overhead:** Adds slight delay to application startup
❌ **Black Box:** Harder to understand what's being captured

## Troubleshooting

### No Traces Appearing in Jaeger

1. **Check if agent loaded:**
   ```bash
   docker logs demo-backend-agent | grep "opentelemetry"
   ```
   Should see: "OpenTelemetry Javaagent started"

2. **Check Jaeger connectivity:**
   ```bash
   curl http://localhost:4318/v1/traces
   ```

3. **Verify environment variables:**
   ```bash
   docker exec demo-backend-agent env | grep OTEL
   ```

### Agent Not Loading

**Error:** "Error opening zip file or JAR manifest missing"
- Make sure the agent JAR is mounted correctly in docker-compose volumes

### Performance Impact

The Java agent adds minimal overhead:
- ~2-5% CPU increase
- ~50-100MB memory increase
- <1ms latency per instrumented call

## Comparison with Spring Boot Starter

| Feature | Java Agent | Spring Boot Starter |
|---------|------------|---------------------|
| Code Changes | None | Required |
| Setup Complexity | Low | Medium |
| Instrumentation Coverage | Comprehensive | Selective |
| Customization | Limited | Extensive |
| Spring Integration | Generic | Native |
| Production Use | Common | Growing |

## Next Steps

- Compare with `springboot-starter-otel` version (manual instrumentation)
- Add custom spans and attributes
- Export to other backends (Zipkin, Tempo, etc.)
- Add metrics and logging

## Resources

- [OpenTelemetry Java Agent Docs](https://opentelemetry.io/docs/instrumentation/java/automatic/)
- [Supported Libraries](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/supported-libraries.md)
- [Configuration Options](https://opentelemetry.io/docs/instrumentation/java/automatic/agent-config/)
