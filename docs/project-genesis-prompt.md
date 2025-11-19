We are going to make a java sprintboot demo app. It needs to have three services - let's make:
1. a React SPA for the frontend - it just needs a few buttons that call endpoints on the 'backend' service
2. the 'backend' service recieves http requests from the frontend and forwards them to the 'upstream' service - like a proxy
3. The upstream service replies to requests with the current timestamp.

This is a very simple app that we will be instrumenting later.

The important thing is that it is a canonical "springboot" app and that the 'backend' service both recieves http requests and makes outgoing http requests. 

Q&A:

### Java version

18+, maybe close to 18 is better. 

### API Contract

    - What endpoints should the frontend call? (e.g., /api/timestamp, /api/health)
    - What HTTP methods? (GET, POST?)
    - Expected request/response formats? (JSON?)
    - Should there be multiple endpoints or just one?

/api/demo and maybe have buttons for POST / GET etc.

I'd also like a button to make continious requests where we can choose the number
of seconds betwwen requests and it constantly requests. We should have /health too
that propogates all the way from the spa to the upstream service via the backend and
shows the health of the connection to both services in the spa 

### Service Communication

REST and no auth

it could help if it transformed it. I thought maybe frontend calls /api/frontend_to_backend and backend transformed it for /api/backend_to_upstream. But totally open to ideas.

### Error Handling

if upstread service is down just reflect it in the health indicatiors on the spa. I guess probably best if the frontend monitors /health on the backend and upstream services directly

### Ports

I'd say 3000-3002

### Build & Run

monorepo. 

Let's make a docker-compose, yes

Build tools? I'm a bit rusty on my Java / sprint boot so you choose