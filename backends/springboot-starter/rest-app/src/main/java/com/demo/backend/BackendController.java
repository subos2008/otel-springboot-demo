package com.demo.backend;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Backend Controller - Demonstrates OpenTelemetry Spring Boot Starter instrumentation
 *
 * This controller showcases different levels of instrumentation:
 * 1. Automatic instrumentation via Spring Boot Starter (no code changes)
 * 2. Enhanced instrumentation using @WithSpan annotation
 * 3. Manual span manipulation using the OpenTelemetry API
 *
 * The Spring Boot Starter automatically instruments:
 * - All @RequestMapping endpoints (creates server spans)
 * - RestTemplate calls (creates client spans with context propagation)
 * - Exception handling (records exceptions in spans)
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class BackendController {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${upstream.service.url}")
    private String upstreamUrl;

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
     * Proxies requests to the upstream service with OpenTelemetry instrumentation.
     *
     * DEMONSTRATION OF @WithSpan ANNOTATION:
     *
     * The @WithSpan annotation creates a custom child span within the automatically-created
     * server span. This is useful when you want to:
     * - Break down a request handler into logical operations
     * - Add custom span names that are more descriptive than method names
     * - Capture specific attributes about the operation
     *
     * The @SpanAttribute annotation automatically adds method parameters to the span as attributes.
     * This helps with debugging and filtering traces in Honeycomb.
     *
     * Without @WithSpan:
     * - Spring Boot Starter still creates a span for the @GetMapping endpoint
     * - RestTemplate call creates a client span automatically
     *
     * With @WithSpan:
     * - An additional "proxy-request" span is created
     * - HTTP method and payload are captured as span attributes
     * - You get more granular visibility into the request processing
     *
     * Note: This method also demonstrates manual span manipulation using Span.current()
     * for adding custom events and attributes beyond what annotations provide.
     */
    @WithSpan("proxy-request")
    private ResponseEntity<Map<String, Object>> proxyRequest(
            @SpanAttribute("http.method") HttpMethod method,
            @SpanAttribute("request.payload") Map<String, Object> payload) {

        // Get the current span for manual instrumentation
        // The Spring Boot Starter makes this span available in the current context
        Span currentSpan = Span.current();

        try {
            String url = upstreamUrl + "/api/backend_to_upstream";

            // Add a custom attribute to the current span
            // This is useful for capturing business logic details
            currentSpan.setAttribute("upstream.url", url);

            // Add an event to mark the start of the upstream call
            // Events are timestamped annotations that help track request lifecycle
            currentSpan.addEvent("starting-upstream-call");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            // RestTemplate call is automatically instrumented by the Spring Boot Starter
            // It will:
            // 1. Create a client span
            // 2. Inject the traceparent header for context propagation
            // 3. Link this span to the upstream service's span (if instrumented)
            ResponseEntity<Map> upstreamResponse = restTemplate.exchange(
                url,
                method,
                entity,
                Map.class
            );

            // Add an event to mark successful completion
            currentSpan.addEvent("upstream-call-completed");

            // Set the span status to OK
            // This helps distinguish successful operations from errors in traces
            currentSpan.setStatus(StatusCode.OK);

            Map<String, Object> response = new HashMap<>();
            response.put("service", "backend");
            response.put("method", method.name());
            response.put("upstream", upstreamResponse.getBody());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Record the exception in the span
            // This is done automatically by Spring Boot Starter, but we're showing it explicitly
            currentSpan.recordException(e);

            // Set the span status to ERROR with a description
            // This makes errors highly visible in Honeycomb's trace view
            currentSpan.setStatus(StatusCode.ERROR, "Failed to connect to upstream service");

            // Add an error event with details
            currentSpan.addEvent("upstream-call-failed");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("service", "backend");
            errorResponse.put("error", "Failed to connect to upstream service");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        }
    }
}
