# Spring Boot Demo Application

A simple three-tier microservices demo application built with Java Spring Boot and React, designed to demonstrate inter-service HTTP communication patterns for observability instrumentation.

## Architecture

```
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│   Frontend  │─────▶│   Backend   │─────▶│  Upstream   │
│  React SPA  │      │   Service   │      │   Service   │
│  Port 3000  │◀─────│  Port 3001  │◀─────│  Port 3002  │
└─────────────┘      └─────────────┘      └─────────────┘
```

### Services

1. **Frontend (Port 3000)**
   - React + TypeScript + Vite
   - Interactive UI with HTTP method buttons (GET, POST, PUT, DELETE)
   - Continuous request mode with configurable intervals
   - Real-time health monitoring dashboard
   - Request history tracking

2. **Backend (Port 3001)**
   - Spring Boot 2.7.6 + Java 17
   - Apache Camel 3.11.5
   - Acts as a proxy/middleware between frontend and upstream
   - Transforms requests from `/api/frontend_to_backend` to `/api/backend_to_upstream`
   - Aggregates health information from upstream service
   - CORS enabled for frontend communication

3. **Upstream (Port 3002)**
   - Spring Boot 2.7.6 + Java 17
   - Apache Camel 3.11.5
   - Provides timestamp data
   - Simple REST API endpoints
   - Health check endpoints

## Prerequisites

### Running with Docker (Recommended)
- Docker 20.10+
- Docker Compose 2.0+

### Running Locally
- Java 17
- Maven 3.9+
- Node.js 18+
- npm or yarn

## Quick Start with Docker

1. **Build and start all services:**
   ```bash
   cd sprintboot-starter
   docker-compose up --build
   ```

2. **Access the application:**
   - Frontend: http://localhost:3000
   - Backend API: http://localhost:3001/api/frontend_to_backend
   - Upstream API: http://localhost:3002/api/backend_to_upstream
   - Backend Health: http://localhost:3001/actuator/health
   - Upstream Health: http://localhost:3002/actuator/health

3. **Stop all services:**
   ```bash
   docker-compose down
   ```

## Running Locally (Without Docker)

### 1. Start Upstream Service

```bash
cd upstream
mvn clean package
mvn spring-boot:run
```

The upstream service will start on port 3002.

### 2. Start Backend Service

```bash
cd backend
mvn clean package
mvn spring-boot:run
```

The backend service will start on port 3001 and connect to the upstream service.

### 3. Start Frontend Service

```bash
cd frontend
npm install
npm run dev
```

The frontend will start on port 3000.

### 4. Access the Application

Open your browser and navigate to http://localhost:3000

## API Documentation

### Backend Service Endpoints

#### Request Proxy Endpoint
- **GET** `/api/frontend_to_backend`
- **POST** `/api/frontend_to_backend`
- **PUT** `/api/frontend_to_backend`
- **DELETE** `/api/frontend_to_backend`

**Response Example:**
```json
{
  "service": "backend",
  "method": "GET",
  "upstream": {
    "timestamp": "2025-11-19T10:30:45.123Z",
    "message": "Hello from upstream",
    "service": "upstream"
  }
}
```

#### Health Check
- **GET** `/actuator/health`

**Response Example:**
```json
{
  "status": "UP",
  "components": {
    "upstreamHealthIndicator": {
      "status": "UP",
      "details": {
        "upstream": "UP",
        "responseTime": "45ms",
        "url": "http://localhost:3002"
      }
    }
  }
}
```

### Upstream Service Endpoints

#### Timestamp Endpoint
- **GET** `/api/backend_to_upstream`
- **POST** `/api/backend_to_upstream`
- **PUT** `/api/backend_to_upstream`
- **DELETE** `/api/backend_to_upstream`

**Response Example:**
```json
{
  "timestamp": "2025-11-19T10:30:45.123Z",
  "message": "Hello from upstream",
  "service": "upstream"
}
```

## Frontend Features

### 1. HTTP Method Buttons
Click any of the four buttons (GET, POST, PUT, DELETE) to send a request through the entire service chain:
- Frontend → Backend → Upstream → Backend → Frontend

### 2. Continuous Request Mode
- Set an interval (in seconds)
- Click "Start" to begin sending requests automatically
- Watch the request counter increment
- Click "Stop" to halt continuous requests

### 3. Health Dashboard
- Real-time health monitoring of backend and upstream services
- Green indicators for healthy services
- Red indicators for unhealthy services
- Response time display
- Auto-refresh every 5 seconds

### 4. Response Display
- Shows the complete response from the latest request
- JSON formatted for easy reading
- Demonstrates the full request flow through all services

### 5. Request History
- Displays the last 10 requests
- Shows timestamp, HTTP method, status code, and response time
- Useful for tracking patterns and debugging

## Testing

### Manual Testing Scenarios

1. **Normal Operation**
   - Start all three services
   - Verify health indicators are green
   - Send GET request and verify response contains data from all services

2. **Service Failure Simulation**
   - Stop upstream service: `docker-compose stop upstream`
   - Observe backend health indicator turns red
   - Backend should handle gracefully with error messages

3. **High-Frequency Requests**
   - Set continuous mode interval to 1 second
   - Start continuous requests
   - Verify all services handle load without crashing

4. **Different HTTP Methods**
   - Test GET, POST, PUT, DELETE buttons
   - Verify each method is correctly passed through the chain

## Project Structure

```
sprintboot-starter/
├── docker-compose.yml
├── README.md
├── .gitignore
├── frontend/
│   ├── Dockerfile
│   ├── package.json
│   ├── tsconfig.json
│   ├── vite.config.ts
│   ├── index.html
│   └── src/
│       ├── main.tsx
│       ├── App.tsx
│       └── index.css
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/com/demo/backend/
│           │   ├── BackendApplication.java
│           │   ├── BackendController.java
│           │   └── UpstreamHealthIndicator.java
│           └── resources/
│               └── application.properties
└── upstream/
    ├── Dockerfile
    ├── pom.xml
    └── src/
        └── main/
            ├── java/com/demo/upstream/
            │   ├── UpstreamApplication.java
            │   └── UpstreamController.java
            └── resources/
                └── application.properties
```

## Configuration

### Environment Variables

#### Backend Service
- `UPSTREAM_SERVICE_URL`: URL of the upstream service (default: `http://localhost:3002`)

#### Frontend Service
- `VITE_BACKEND_URL`: URL of the backend service (default: `http://localhost:3001`)
- `VITE_UPSTREAM_URL`: URL of the upstream service (default: `http://localhost:3002`)

## Troubleshooting

### Services won't start
- Ensure ports 3000, 3001, and 3002 are not in use
- Check Docker is running (for Docker deployment)
- Verify Java 21+ is installed (for local deployment)

### Backend can't connect to upstream
- Verify upstream service is running
- Check the `UPSTREAM_SERVICE_URL` configuration
- Review backend logs for connection errors

### Frontend shows all services as DOWN
- Verify backend and upstream services are running
- Check browser console for CORS errors
- Ensure you're accessing the frontend at http://localhost:3000

### Maven build fails
- Ensure Java 17 is installed: `java -version`
- Try cleaning: `mvn clean`
- Check internet connection for dependency downloads

### npm install fails
- Ensure Node.js 18+ is installed: `node --version`
- Try clearing npm cache: `npm cache clean --force`
- Delete `node_modules` and `package-lock.json`, then retry

## Future Enhancements

This is the starter version. Future versions will include:
- OpenTelemetry instrumentation
- Distributed tracing
- Metrics collection
- Logging aggregation
- Request ID propagation
- Circuit breaker patterns
- Retry logic

## License

This is a demo application for educational and development purposes.
