import { useState, useEffect, useRef } from 'react'
import axios from 'axios'

const BACKENDS = {
  'starter-rest': {
    url: 'http://localhost:3010',
    name: 'Spring Boot Starter - REST',
    path: 'backends/springboot-starter/rest-app',
    type: 'Spring Boot Starter'
  },
  'starter-camel-rest': {
    url: 'http://localhost:3012',
    name: 'Spring Boot Starter - Camel REST',
    path: 'backends/springboot-starter/camel-rest-app',
    type: 'Spring Boot Starter'
  },
  'starter-camel-rest-dev': {
    url: 'http://localhost:3013',
    name: 'Spring Boot Starter - Camel REST (DEV)',
    path: 'backends/springboot-starter/camel-rest-app-dev',
    type: 'Spring Boot Starter'
  },
  'agent-rest': {
    url: 'http://localhost:3011',
    name: 'OTEL Java Agent - REST',
    path: 'backends/otel-java-agent/rest-app',
    type: 'OTEL Java Agent'
  },
  'agent-camel-rest': {
    url: 'http://localhost:3014',
    name: 'OTEL Java Agent - Camel REST',
    path: 'backends/otel-java-agent/camel-rest-app',
    type: 'OTEL Java Agent'
  },
}

const UPSTREAM_URL = 'http://localhost:3002'

interface HealthStatus {
  backend: 'UP' | 'DOWN' | 'CHECKING'
  upstream: 'UP' | 'DOWN' | 'CHECKING'
  backendResponseTime?: number
  upstreamResponseTime?: number
}

interface RequestHistoryItem {
  timestamp: string
  method: string
  status: number
  responseTime: number
}

interface ApiResponse {
  service?: string
  method?: string
  upstream?: any
  timestamp?: string
  message?: string
  error?: string
}

function App() {
  const [selectedBackend, setSelectedBackend] = useState<'starter-rest' | 'starter-camel-rest' | 'starter-camel-rest-dev' | 'agent-rest' | 'agent-camel-rest'>('starter-rest')
  const [response, setResponse] = useState<ApiResponse | null>(null)
  const [health, setHealth] = useState<HealthStatus>({
    backend: 'CHECKING',
    upstream: 'CHECKING',
  })
  const [history, setHistory] = useState<RequestHistoryItem[]>([])
  const [isRunning, setIsRunning] = useState(false)
  const [requestInterval, setRequestInterval] = useState(5)
  const [requestCount, setRequestCount] = useState(0)
  const intervalRef = useRef<number | null>(null)

  const BACKEND_URL = BACKENDS[selectedBackend].url

  // Health check function
  const checkHealth = async () => {
    // Check backend
    try {
      const startTime = Date.now()
      await axios.get(`${BACKEND_URL}/actuator/health`)
      const responseTime = Date.now() - startTime
      setHealth((prev) => ({ ...prev, backend: 'UP', backendResponseTime: responseTime }))
    } catch {
      setHealth((prev) => ({ ...prev, backend: 'DOWN' }))
    }

    // Check upstream
    try {
      const startTime = Date.now()
      await axios.get(`${UPSTREAM_URL}/actuator/health`)
      const responseTime = Date.now() - startTime
      setHealth((prev) => ({ ...prev, upstream: 'UP', upstreamResponseTime: responseTime }))
    } catch {
      setHealth((prev) => ({ ...prev, upstream: 'DOWN' }))
    }
  }

  // Initial health check and periodic updates
  useEffect(() => {
    checkHealth()
    const healthInterval = window.setInterval(checkHealth, 5000)
    return () => window.clearInterval(healthInterval)
  }, [selectedBackend])

  // Make API request
  const makeRequest = async (method: string) => {
    const startTime = Date.now()
    try {
      const config = {
        method: method.toLowerCase(),
        url: `${BACKEND_URL}/api/frontend_to_backend`,
        data: method !== 'GET' && method !== 'DELETE' ? { request: `from frontend using ${method}` } : undefined,
      }

      const res = await axios(config)
      const responseTime = Date.now() - startTime

      setResponse(res.data)

      // Add to history
      const historyItem: RequestHistoryItem = {
        timestamp: new Date().toLocaleTimeString(),
        method,
        status: res.status,
        responseTime,
      }
      setHistory((prev) => [historyItem, ...prev].slice(0, 10))
    } catch (error: any) {
      const responseTime = Date.now() - startTime
      setResponse({
        error: 'Request failed',
        message: error.message,
      })

      const historyItem: RequestHistoryItem = {
        timestamp: new Date().toLocaleTimeString(),
        method,
        status: error.response?.status || 0,
        responseTime,
      }
      setHistory((prev) => [historyItem, ...prev].slice(0, 10))
    }
  }

  // Start continuous requests
  const startContinuous = () => {
    setIsRunning(true)
    setRequestCount(0)

    // Make first request immediately
    makeRequest('GET')
    setRequestCount(1)

    // Then continue at intervals
    intervalRef.current = window.setInterval(() => {
      makeRequest('GET')
      setRequestCount((prev) => prev + 1)
    }, requestInterval * 1000)
  }

  // Stop continuous requests
  const stopContinuous = () => {
    setIsRunning(false)
    if (intervalRef.current) {
      window.clearInterval(intervalRef.current)
      intervalRef.current = null
    }
  }

  return (
    <>
      <h1>Spring Boot Demo App</h1>

      <div className="container">
        {/* Backend Selector */}
        <div className="card" style={{ gridColumn: 'span 2' }}>
          <h2>Backend Version</h2>
          <p style={{ fontSize: '0.9rem', color: '#666', marginBottom: '15px' }}>
            All backends are instrumented with OpenTelemetry. Choose which version to test.
          </p>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 400px', gap: '20px' }}>
            {/* Left: Backend Groups */}
            <div>
              {/* Spring Boot Starter Group */}
              <div style={{ marginBottom: '15px' }}>
                <h3 style={{
                  fontSize: '0.9rem',
                  color: '#2c3e50',
                  marginBottom: '8px',
                  fontWeight: '600',
                  borderLeft: '4px solid #3498db',
                  paddingLeft: '10px'
                }}>
                  Spring Boot Starter
                </h3>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '6px' }}>
                  <button
                    className={selectedBackend === 'starter-rest' ? 'btn-primary' : 'btn-get'}
                    onClick={() => setSelectedBackend('starter-rest')}
                    style={{ fontSize: '0.85rem', padding: '8px 10px' }}
                  >
                    REST
                  </button>
                  <button
                    className={selectedBackend === 'starter-camel-rest' ? 'btn-primary' : 'btn-get'}
                    onClick={() => setSelectedBackend('starter-camel-rest')}
                    style={{ fontSize: '0.85rem', padding: '8px 10px' }}
                  >
                    Camel REST
                  </button>
                  <button
                    className={selectedBackend === 'starter-camel-rest-dev' ? 'btn-primary' : 'btn-get'}
                    onClick={() => setSelectedBackend('starter-camel-rest-dev')}
                    style={{ fontSize: '0.85rem', padding: '8px 10px' }}
                  >
                    Camel DEV
                  </button>
                </div>
              </div>

              {/* OTEL Java Agent Group */}
              <div>
                <h3 style={{
                  fontSize: '0.9rem',
                  color: '#2c3e50',
                  marginBottom: '8px',
                  fontWeight: '600',
                  borderLeft: '4px solid #9b59b6',
                  paddingLeft: '10px'
                }}>
                  OTEL Java Agent
                </h3>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '6px' }}>
                  <button
                    className={selectedBackend === 'agent-rest' ? 'btn-primary' : 'btn-get'}
                    onClick={() => setSelectedBackend('agent-rest')}
                    style={{ fontSize: '0.85rem', padding: '8px 10px' }}
                  >
                    REST
                  </button>
                  <button
                    className={selectedBackend === 'agent-camel-rest' ? 'btn-primary' : 'btn-get'}
                    onClick={() => setSelectedBackend('agent-camel-rest')}
                    style={{ fontSize: '0.85rem', padding: '8px 10px' }}
                  >
                    Camel REST
                  </button>
                </div>
              </div>
            </div>

            {/* Right: Selected Backend Details */}
            <div style={{
              padding: '15px',
              backgroundColor: '#f8f9fa',
              borderRadius: '5px',
              border: '1px solid #e0e0e0',
              height: 'fit-content'
            }}>
              <div style={{ marginBottom: '10px' }}>
                <div style={{ fontWeight: '600', color: '#2c3e50', fontSize: '0.9rem', marginBottom: '5px' }}>
                  {BACKENDS[selectedBackend].name}
                </div>
                <span style={{
                  padding: '2px 8px',
                  backgroundColor: BACKENDS[selectedBackend].type === 'Spring Boot Starter' ? '#3498db' : '#9b59b6',
                  color: 'white',
                  borderRadius: '3px',
                  fontSize: '0.7rem',
                  fontWeight: '600'
                }}>
                  {BACKENDS[selectedBackend].type}
                </span>
              </div>
              <div style={{ fontSize: '0.8rem', color: '#555', lineHeight: '1.8' }}>
                <div><strong>Port:</strong> {new URL(BACKENDS[selectedBackend].url).port}</div>
                <div style={{ marginTop: '5px' }}>
                  <strong>Path:</strong>
                  <div style={{
                    backgroundColor: '#e8e8e8',
                    padding: '4px 6px',
                    borderRadius: '3px',
                    fontSize: '0.75rem',
                    fontFamily: 'monospace',
                    marginTop: '3px',
                    wordBreak: 'break-all'
                  }}>
                    {BACKENDS[selectedBackend].path}
                  </div>
                </div>
                <div style={{ marginTop: '5px' }}>
                  <strong>URL:</strong>
                  <div style={{
                    backgroundColor: '#e8e8e8',
                    padding: '4px 6px',
                    borderRadius: '3px',
                    fontSize: '0.75rem',
                    fontFamily: 'monospace',
                    marginTop: '3px'
                  }}>
                    {BACKENDS[selectedBackend].url}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Health Dashboard */}
        <div className="card">
          <h2>Service Health</h2>
          <div className="health-status">
            <div className={`health-item health-${health.backend.toLowerCase()}`}>
              <div>Backend (port {new URL(BACKEND_URL).port})</div>
              <div style={{ fontSize: '0.9rem', marginTop: '5px' }}>
                {health.backend}
                {health.backendResponseTime && ` (${health.backendResponseTime}ms)`}
              </div>
            </div>
            <div className={`health-item health-${health.upstream.toLowerCase()}`}>
              <div>Upstream (port {new URL(UPSTREAM_URL).port})</div>
              <div style={{ fontSize: '0.9rem', marginTop: '5px' }}>
                {health.upstream}
                {health.upstreamResponseTime && ` (${health.upstreamResponseTime}ms)`}
              </div>
            </div>
          </div>
        </div>

        {/* HTTP Method Buttons */}
        <div className="card">
          <h2>HTTP Requests</h2>
          <p style={{ fontSize: '0.9rem', color: '#666', marginBottom: '15px' }}>
            Send a request through the entire service chain: Frontend → Backend → Upstream
          </p>
          <div className="button-group">
            <button className="btn-get" onClick={() => makeRequest('GET')}>
              GET
            </button>
            <button className="btn-post" onClick={() => makeRequest('POST')}>
              POST
            </button>
            <button className="btn-put" onClick={() => makeRequest('PUT')}>
              PUT
            </button>
            <button className="btn-delete" onClick={() => makeRequest('DELETE')}>
              DELETE
            </button>
          </div>
        </div>

        {/* Continuous Request Mode */}
        <div className="card">
          <h2>Continuous Request Mode</h2>
          <p style={{ fontSize: '0.9rem', color: '#666', marginBottom: '15px' }}>
            Automatically send GET requests at regular intervals to test sustained load
          </p>
          <div className="continuous-controls">
            <label>
              Interval (seconds):
              <input
                type="number"
                min="1"
                value={requestInterval}
                onChange={(e) => setRequestInterval(Number(e.target.value))}
                disabled={isRunning}
              />
            </label>
            {!isRunning ? (
              <button className="btn-primary" onClick={startContinuous}>
                Start
              </button>
            ) : (
              <button className="btn-danger" onClick={stopContinuous}>
                Stop
              </button>
            )}
            {isRunning && <span className="running-indicator"></span>}
          </div>
          <div className="counter">Total Requests: {requestCount}</div>
        </div>

        {/* Response Display */}
        <div className="card">
          <h2>Latest Response</h2>
          <div className="response-display">
            {response ? (
              <pre>{JSON.stringify(response, null, 2)}</pre>
            ) : (
              <p style={{ color: '#999' }}>No requests made yet. Click a button to make a request.</p>
            )}
          </div>
        </div>

        {/* Request History */}
        <div className="card" style={{ gridColumn: 'span 2' }}>
          <h2>Request History (Last 10)</h2>
          <div className="request-history">
            {history.length === 0 ? (
              <p style={{ color: '#999' }}>No request history yet.</p>
            ) : (
              history.map((item, index) => (
                <div key={index} className="history-item">
                  <strong>{item.timestamp}</strong> - {item.method} - Status: {item.status} - Response Time: {item.responseTime}ms
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </>
  )
}

export default App
