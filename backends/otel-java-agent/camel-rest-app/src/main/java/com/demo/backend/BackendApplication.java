package com.demo.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * Backend Application with Apache Camel Integration
 *
 * This application demonstrates Apache Camel's routing capabilities with OpenTelemetry tracing.
 *
 * CAMEL OPENTELEMETRY CONTEXT PROPAGATION:
 *
 * With camel-opentelemetry on the classpath:
 * ✅ Camel automatically propagates trace context across all routes
 * ✅ HTTP component automatically injects traceparent headers
 * ✅ Each route step creates its own span in the trace
 * ✅ All spans are correctly linked to show the complete request flow
 *
 * Camel's HTTP component handles the main business logic HTTP calls with
 * superior routing capabilities and automatic instrumentation.
 *
 * Benefits over RestTemplate:
 * - Declarative routing patterns (routes defined separately from business logic)
 * - Built-in Enterprise Integration Patterns (EIP)
 * - Automatic retry, error handling, and circuit breakers
 * - Better observability with per-step tracing
 *
 * Note: We still provide a RestTemplate bean for the health check indicator,
 * but the main request flow uses Camel.
 */
@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    /**
     * Provides a RestTemplate bean for health checks.
     * The main request flow uses Camel's HTTP component instead.
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
