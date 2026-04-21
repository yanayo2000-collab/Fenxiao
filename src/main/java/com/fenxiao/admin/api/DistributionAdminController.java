package com.fenxiao.admin.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/distribution")
public class DistributionAdminController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "module", "distribution-admin",
                "status", "ok",
                "phase", "mvp-bootstrap"
        );
    }
}
