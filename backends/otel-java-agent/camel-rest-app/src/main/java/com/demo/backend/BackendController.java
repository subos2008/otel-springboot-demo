package com.demo.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Backend Controller - Demonstrates Apache Camel with OpenTelemetry Java Agent instrumentation
 *
 * This controller showcases:
 * 1. Apache Camel integration for routing patterns
 * 2. ProducerTemplate for sending messages to Camel routes
 * 3. Automatic OpenTelemetry tracing via Java Agent
 * 4. Zero-code instrumentation approach
 *
 * The OpenTelemetry Java Agent automatically instruments:
 * - All HTTP endpoints (Spring MVC)
 * - All Camel routes (via camel-opentelemetry component)
 * - HTTP component calls (creates client spans with context propagation)
 * - Route exchanges (propagates trace context)
 *
 * No manual instrumentation code (@WithSpan, Span.current(), etc.) is needed.
 * The agent handles everything automatically via bytecode instrumentation.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class BackendController {

    @Autowired
    private ProducerTemplate producerTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/frontend_to_backend")
    public ResponseEntity<Map<String, Object>> getRequest() {
        return proxyRequest(HttpMethod.GET, null);
    }

    @PostMapping("/frontend_to_backend")
    public ResponseEntity<Map<String, Object>> postRequest(@RequestBody(required = false) Map<String, Object> payload) {
        return proxyRequest(HttpMethod.POST, payload);
    }

    @PutMapping("/frontend_to_backend")
    public ResponseEntity<Map<String, Object>> putRequest(@RequestBody(required = false) Map<String, Object> payload) {
        return proxyRequest(HttpMethod.PUT, payload);
    }

    @DeleteMapping("/frontend_to_backend")
    public ResponseEntity<Map<String, Object>> deleteRequest() {
        return proxyRequest(HttpMethod.DELETE, null);
    }

    /**
     * Proxies requests to the upstream service using Apache Camel with OpenTelemetry Java Agent.
     *
     * DEMONSTRATION OF JAVA AGENT INSTRUMENTATION:
     *
     * This method uses Camel's ProducerTemplate to send messages to a Camel route.
     * Unlike the Spring Boot Starter approach, this version requires NO manual
     * instrumentation code whatsoever.
     *
     * The Java Agent automatically creates spans for:
     * - The HTTP request to this endpoint (Spring MVC instrumentation)
     * - The ProducerTemplate call (Camel instrumentation)
     * - Route processing (direct:proxyRequest)
     * - HTTP client calls (to upstream)
     * - Each step in the route pipeline
     *
     * All tracing happens automatically with zero code changes needed.
     */
    private ResponseEntity<Map<String, Object>> proxyRequest(
            HttpMethod method,
            Map<String, Object> payload) {

        try {
            // Prepare headers for Camel route
            Map<String, Object> headers = new HashMap<>();
            headers.put("HTTP_METHOD", method.name());
            headers.put("Content-Type", "application/json");

            // Send message to Camel route using ProducerTemplate
            // The route will:
            // 1. Receive the message at direct:proxyRequest
            // 2. Route it to the HTTP endpoint
            // 3. Return the upstream response as JSON string
            // All of this is automatically traced by the Java Agent
            String responseJson = producerTemplate.requestBodyAndHeaders(
                "direct:proxyRequest",
                payload,
                headers,
                String.class
            );

            // Parse the JSON string response
            @SuppressWarnings("unchecked")
            Map<String, Object> upstreamResponse = objectMapper.readValue(responseJson, Map.class);

            Map<String, Object> response = new HashMap<>();
            response.put("service", "backend-agent-camel");
            response.put("method", method.name());
            response.put("upstream", upstreamResponse);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Log the full exception for debugging
            e.printStackTrace();

            // Java Agent automatically records exceptions in spans
            // No manual exception recording needed

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("service", "backend-agent-camel");
            errorResponse.put("error", "Failed to connect to upstream service");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("exceptionType", e.getClass().getName());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        }
    }
}
