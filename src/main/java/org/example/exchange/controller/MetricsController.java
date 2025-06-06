package org.example.exchange.controller;

import org.example.exchange.aspect.ApiMetricsAspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/metrics")
public class MetricsController {

    @Autowired
    private ApiMetricsAspect apiMetricsAspect;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getMetrics() {
        return ResponseEntity.ok(apiMetricsAspect.getWebClientMetrics());
    }

}

