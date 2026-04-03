package com.zorvyn.financedataprocessing.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, String> health() {
        // Keep the health response minimal so checks stay cheap and predictable.
        return Map.of("status", "ok");
    }
}
