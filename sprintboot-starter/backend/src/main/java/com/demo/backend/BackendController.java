package com.demo.backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

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

    private ResponseEntity<Map<String, Object>> proxyRequest(HttpMethod method, Map<String, Object> payload) {
        try {
            String url = upstreamUrl + "/api/backend_to_upstream";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> upstreamResponse = restTemplate.exchange(
                url,
                method,
                entity,
                Map.class
            );

            Map<String, Object> response = new HashMap<>();
            response.put("service", "backend");
            response.put("method", method.name());
            response.put("upstream", upstreamResponse.getBody());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("service", "backend");
            errorResponse.put("error", "Failed to connect to upstream service");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        }
    }
}
