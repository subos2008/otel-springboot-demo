package com.demo.backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class UpstreamHealthIndicator implements HealthIndicator {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${upstream.service.url}")
    private String upstreamUrl;

    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();
            String url = upstreamUrl + "/actuator/health";

            restTemplate.getForObject(url, String.class);

            long responseTime = System.currentTimeMillis() - startTime;

            return Health.up()
                    .withDetail("upstream", "UP")
                    .withDetail("responseTime", responseTime + "ms")
                    .withDetail("url", upstreamUrl)
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("upstream", "DOWN")
                    .withDetail("error", e.getMessage())
                    .withDetail("url", upstreamUrl)
                    .build();
        }
    }
}
