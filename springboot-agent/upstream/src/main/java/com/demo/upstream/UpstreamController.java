package com.demo.upstream;

import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UpstreamController {

    @GetMapping("/backend_to_upstream")
    public Map<String, Object> getTimestamp() {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("message", "Hello from upstream");
        response.put("service", "upstream");
        return response;
    }

    @PostMapping("/backend_to_upstream")
    public Map<String, Object> postTimestamp(@RequestBody(required = false) Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("message", "Hello from upstream (POST)");
        response.put("service", "upstream");
        response.put("receivedPayload", payload);
        return response;
    }

    @PutMapping("/backend_to_upstream")
    public Map<String, Object> putTimestamp(@RequestBody(required = false) Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("message", "Hello from upstream (PUT)");
        response.put("service", "upstream");
        response.put("receivedPayload", payload);
        return response;
    }

    @DeleteMapping("/backend_to_upstream")
    public Map<String, Object> deleteTimestamp() {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("message", "Hello from upstream (DELETE)");
        response.put("service", "upstream");
        return response;
    }
}
