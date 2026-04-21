package com.fenxiao.distribution.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/distribution")
public class DistributionController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "module", "distribution",
                "status", "ok",
                "phase", "mvp-bootstrap"
        );
    }
}
