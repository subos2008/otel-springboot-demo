package com.demo.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    /**
     * Creates a RestTemplate bean for making HTTP requests to upstream services.
     *
     * CRITICAL FOR OPENTELEMETRY CONTEXT PROPAGATION:
     *
     * We MUST use RestTemplateBuilder instead of "new RestTemplate()" because:
     *
     * 1. RestTemplateBuilder is Spring-managed and goes through Spring's bean post-processors
     * 2. OpenTelemetry's instrumentation hooks into these post-processors to add interceptors
     * 3. These interceptors automatically inject the "traceparent" HTTP header into outgoing requests
     * 4. This header contains the trace ID and span ID, enabling distributed tracing
     *
     * If you use "new RestTemplate()" directly:
     * ❌ Context propagation will NOT work
     * ❌ Upstream service will start a new trace instead of continuing the existing one
     * ❌ You'll see disconnected traces in Honeycomb instead of a single distributed trace
     *
     * With RestTemplateBuilder:
     * ✅ OpenTelemetry automatically adds the traceparent header
     * ✅ Upstream service continues the trace (if also instrumented)
     * ✅ You get a complete view of the request flow across all services
     *
     * This is a common mistake when implementing OpenTelemetry Spring Boot Starter!
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
