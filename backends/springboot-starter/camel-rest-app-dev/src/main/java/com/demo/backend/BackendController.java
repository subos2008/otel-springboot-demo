package com.demo.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Backend Controller - Demonstrates Apache Camel with OpenTelemetry instrumentation
 *
 * This controller showcases:
 * 1. Apache Camel integration for routing patterns
 * 2. ProducerTemplate for sending messages to Camel routes
 * 3. Automatic OpenTelemetry tracing via camel-opentelemetry
 * 4. Enhanced instrumentation using @WithSpan annotation
 * 5. Manual span manipulation using the OpenTelemetry API
 *
 * The camel-opentelemetry component automatically instruments:
 * - All Camel routes (creates spans for each route step)
 * - HTTP component calls (creates client spans with context propagation)
 * - Route exchanges (propagates trace context)
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
     * Proxies requests to the upstream service using Apache Camel with OpenTelemetry instrumentation.
     *
     * DEMONSTRATION OF APACHE CAMEL INTEGRATION:
     *
     * This method uses Camel's ProducerTemplate to send messages to a Camel route instead of
     * using RestTemplate directly. This showcases:
     * - Decoupling HTTP logic from controller code
     * - Enterprise Integration Patterns (EIP) via Camel routes
     * - Automatic tracing of Camel routes via camel-opentelemetry
     * - Message-based communication patterns
     *
     * The @WithSpan annotation creates a custom child span for additional visibility.
     * Camel's OpenTelemetry integration creates additional spans for:
     * - Route processing (direct:proxyRequest)
     * - HTTP client calls (to upstream)
     * - Each step in the route pipeline
     *
     * Note: This method also demonstrates manual span manipulation using Span.current()
     * for adding custom events and attributes beyond what Camel automatically provides.
     */
    @WithSpan("proxy-request")
    private ResponseEntity<Map<String, Object>> proxyRequest(
            @SpanAttribute("http.method") HttpMethod method,
            @SpanAttribute("request.payload") Map<String, Object> payload) {

        // Get the current span for manual instrumentation
        Span currentSpan = Span.current();

        try {
            // Add a custom attribute to the current span
            currentSpan.setAttribute("camel.route", "direct:proxyRequest");

            // Add an event to mark the start of the Camel route call
            currentSpan.addEvent("starting-camel-route");

            // Prepare headers for Camel route
            Map<String, Object> headers = new HashMap<>();
            headers.put("HTTP_METHOD", method.name());
            headers.put("Content-Type", "application/json");

            // Send message to Camel route using ProducerTemplate
            // The route will:
            // 1. Receive the message at direct:proxyRequest
            // 2. Route it to the HTTP endpoint
            // 3. Return the upstream response as JSON string
            // All of this is automatically traced by camel-opentelemetry
            String responseJson = producerTemplate.requestBodyAndHeaders(
                "direct:proxyRequest",
                payload,
                headers,
                String.class
            );

            // Parse the JSON string response
            @SuppressWarnings("unchecked")
            Map<String, Object> upstreamResponse = objectMapper.readValue(responseJson, Map.class);

            // Add an event to mark successful completion
            currentSpan.addEvent("camel-route-completed");

            // Set the span status to OK
            currentSpan.setStatus(StatusCode.OK);

            Map<String, Object> response = new HashMap<>();
            response.put("service", "backend-camel");
            response.put("method", method.name());
            response.put("upstream", upstreamResponse);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Log the full exception for debugging
            e.printStackTrace();

            // Record the exception in the span
            currentSpan.recordException(e);

            // Set the span status to ERROR with a description
            currentSpan.setStatus(StatusCode.ERROR, "Camel route failed to connect to upstream service");

            // Add an error event with details
            currentSpan.addEvent("camel-route-failed");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("service", "backend-camel");
            errorResponse.put("error", "Failed to connect to upstream service");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("exceptionType", e.getClass().getName());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        }
    }
}
