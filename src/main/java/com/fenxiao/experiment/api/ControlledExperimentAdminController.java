package com.fenxiao.experiment.api;

import com.fenxiao.common.security.DistributionAccessGuard;
import com.fenxiao.experiment.dto.*;
import com.fenxiao.experiment.service.ControlledExperimentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/experiments")
public class ControlledExperimentAdminController {
    private final DistributionAccessGuard access;
    private final ControlledExperimentService service;

    public ControlledExperimentAdminController(DistributionAccessGuard access, ControlledExperimentService service) { this.access = access; this.service = service; }

    @PostMapping
    public Map<String,Object> create(@RequestHeader(value="X-Admin-Token",required=false) String token, @RequestHeader(value="X-Admin-Session",required=false) String session, @Valid @RequestBody CreateExperimentRequest request) {
        var principal = access.assertAdminWriteAccess(token, session); return Map.of("experimentId", service.create(request, principal.accountId()));
    }
    @PostMapping("/{code}/status")
    public Map<String,String> status(@RequestHeader(value="X-Admin-Token",required=false) String token, @RequestHeader(value="X-Admin-Session",required=false) String session, @PathVariable String code, @Valid @RequestBody ExperimentStatusRequest request) {
        var principal = access.assertAdminWriteAccess(token, session); service.changeStatus(code, request, principal.accountId()); return Map.of("experimentCode", code, "status", request.status().trim().toUpperCase());
    }
    @PostMapping("/{code}/participants")
    public Map<String,Object> enroll(@RequestHeader(value="X-Admin-Token",required=false) String token, @RequestHeader(value="X-Admin-Session",required=false) String session, @PathVariable String code, @Valid @RequestBody EnrollParticipantRequest request) {
        var principal = access.assertAdminWriteAccess(token, session); return service.enroll(code, request, principal.accountId());
    }
    @PostMapping("/{code}/participants/{userId}/status")
    public Map<String,String> participantStatus(@RequestHeader(value="X-Admin-Token",required=false) String token, @RequestHeader(value="X-Admin-Session",required=false) String session, @PathVariable String code, @PathVariable long userId, @Valid @RequestBody ExperimentStatusRequest request) {
        var principal = access.assertAdminWriteAccess(token, session); service.changeParticipantStatus(code, userId, request, principal.accountId()); return Map.of("status", request.status().trim().toUpperCase());
    }
    @PostMapping("/{code}/metrics")
    public Map<String,Object> metric(@RequestHeader(value="X-Admin-Token",required=false) String token, @RequestHeader(value="X-Admin-Session",required=false) String session, @PathVariable String code, @Valid @RequestBody ExperimentMetricEventRequest request) {
        access.assertAdminWriteAccess(token, session); return Map.of("accepted", service.recordMetric(code, request));
    }
    @GetMapping("/{code}/dashboard")
    public Map<String,Object> dashboard(@RequestHeader(value="X-Admin-Token",required=false) String token, @RequestHeader(value="X-Admin-Session",required=false) String session, @PathVariable String code) {
        access.assertAdminAccess(token, session); return service.dashboard(code);
    }
}
