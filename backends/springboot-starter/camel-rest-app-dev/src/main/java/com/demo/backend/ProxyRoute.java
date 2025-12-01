package com.demo.backend;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Apache Camel Route - Demonstrates routing patterns with OpenTelemetry integration
 *
 * This route showcases:
 * 1. Direct endpoints for synchronous in-memory routing
 * 2. HTTP component for making HTTP calls
 * 3. Content-based routing and transformation
 * 4. Automatic OpenTelemetry tracing via camel-opentelemetry
 *
 * Apache Camel automatically instruments routes when camel-opentelemetry is present:
 * - Creates spans for each route step
 * - Propagates trace context across endpoints
 * - Records route metadata (endpoint URIs, message headers)
 */
@Component
public class ProxyRoute extends RouteBuilder {

    @Value("${upstream.service.url}")
    private String upstreamUrl;

    @Override
    public void configure() throws Exception {
        /**
         * Direct endpoint "proxyRequest" - receives requests from BackendController
         *
         * Flow:
         * 1. Receives message with headers: HTTP_METHOD, PAYLOAD
         * 2. Routes to HTTP endpoint using dynamic URI
         * 3. Returns upstream response as JSON
         *
         * The direct: endpoint is synchronous and in-memory, perfect for
         * request/response patterns where the controller needs a reply.
         */
        from("direct:proxyRequest")
            .routeId("proxy-to-upstream")
            .log("Camel route processing ${header.HTTP_METHOD} request")

            // Set the HTTP method dynamically based on header
            .setHeader("CamelHttpMethod", simple("${header.HTTP_METHOD}"))

            // Set Content-Type for requests with body
            .setHeader("Content-Type", constant("application/json"))

            // Route to HTTP endpoint - Camel will automatically:
            // - Make the HTTP call
            // - Propagate trace context via W3C headers
            // - Create a client span
            // - Handle request/response serialization
            .to(upstreamUrl + "/api/backend_to_upstream?bridgeEndpoint=true&throwExceptionOnFailure=false")

            .log("Camel route completed with status: ${header.CamelHttpResponseCode}")

            // Convert body to String for debugging
            .convertBodyTo(String.class);
    }
}
