import { useState, useEffect, useRef } from 'react'
import axios from 'axios'

const BACKEND_URL = 'http://localhost:3001'
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
  }, [])

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
        {/* Health Dashboard */}
        <div className="card">
          <h2>Service Health</h2>
          <div className="health-status">
            <div className={`health-item health-${health.backend.toLowerCase()}`}>
              <div>Backend</div>
              <div style={{ fontSize: '0.9rem', marginTop: '5px' }}>
                {health.backend}
                {health.backendResponseTime && ` (${health.backendResponseTime}ms)`}
              </div>
            </div>
            <div className={`health-item health-${health.upstream.toLowerCase()}`}>
              <div>Upstream</div>
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
