package com.fenxiao.admin.api;

import com.fenxiao.common.security.DistributionAccessGuard;
import com.fenxiao.distribution.api.dto.WithdrawalPaymentRequest;
import com.fenxiao.distribution.api.dto.WithdrawalReconciliationRequest;
import com.fenxiao.distribution.entity.WithdrawRequest;
import com.fenxiao.distribution.service.WithdrawRequestService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/distribution/withdrawal-workflow")
public class WithdrawalWorkflowAdminController {
    private final DistributionAccessGuard accessGuard;
    private final WithdrawRequestService service;

    public WithdrawalWorkflowAdminController(DistributionAccessGuard accessGuard, WithdrawRequestService service) {
        this.accessGuard = accessGuard;
        this.service = service;
    }

    @PostMapping("/{requestNo}/approve-for-payment")
    public Map<String, Object> approve(@RequestHeader(value="X-Admin-Token", required=false) String token,
                                      @RequestHeader(value="X-Admin-Session", required=false) String session,
                                      @PathVariable String requestNo,
                                      @RequestBody(required=false) Map<String, String> body) {
        var principal = accessGuard.assertFinanceAccess(token, session);
        return response(service.approveForPayment(requestNo, principal.accountId(), principal.role(), body == null ? null : body.get("remark")));
    }

    @PostMapping("/{requestNo}/payment-success")
    public Map<String, Object> paymentSuccess(@RequestHeader(value="X-Admin-Token", required=false) String token,
                                             @RequestHeader(value="X-Admin-Session", required=false) String session,
                                             @PathVariable String requestNo,
                                             @Valid @RequestBody WithdrawalPaymentRequest request) {
        var principal = accessGuard.assertFinanceAccess(token, session);
        return response(service.recordPaymentSuccess(requestNo, principal.accountId(), principal.role(), request.paymentChannel(), request.paymentReference(), request.evidenceUri(), request.evidenceHash()));
    }

    @PostMapping("/{requestNo}/payment-failure")
    public Map<String, Object> paymentFailure(@RequestHeader(value="X-Admin-Token", required=false) String token,
                                             @RequestHeader(value="X-Admin-Session", required=false) String session,
                                             @PathVariable String requestNo,
                                             @Valid @RequestBody WithdrawalPaymentRequest request) {
        var principal = accessGuard.assertFinanceAccess(token, session);
        return response(service.recordPaymentFailure(requestNo, principal.accountId(), principal.role(), request.paymentChannel(), request.paymentReference(), request.evidenceUri(), request.evidenceHash(), request.failureReason()));
    }

    @PostMapping("/{requestNo}/reverse")
    public Map<String, Object> reverse(@RequestHeader(value="X-Admin-Token", required=false) String token,
                                      @RequestHeader(value="X-Admin-Session", required=false) String session,
                                      @PathVariable String requestNo,
                                      @RequestBody Map<String, String> body) {
        var principal = accessGuard.assertFinanceAccess(token, session);
        return response(service.reversePayment(requestNo, principal.accountId(), principal.role(), body.get("reason"), body.get("currencyCode")));
    }

    @PostMapping("/{requestNo}/reconcile")
    public Map<String, String> reconcile(@RequestHeader(value="X-Admin-Token", required=false) String token,
                                        @RequestHeader(value="X-Admin-Session", required=false) String session,
                                        @PathVariable String requestNo,
                                        @Valid @RequestBody WithdrawalReconciliationRequest request) {
        var principal = accessGuard.assertFinanceAccess(token, session);
        service.reconcile(requestNo, principal.accountId(), request.status(), request.externalReference(), request.externalAmount(), request.currencyCode(), request.details());
        return Map.of("requestNo", requestNo, "reconciliationStatus", request.status().trim().toUpperCase());
    }

    @GetMapping("/{requestNo}/history")
    public List<Map<String, Object>> history(@RequestHeader(value="X-Admin-Token", required=false) String token,
                                             @RequestHeader(value="X-Admin-Session", required=false) String session,
                                             @PathVariable String requestNo) {
        accessGuard.assertFinanceAccess(token, session);
        return service.workflowHistory(requestNo);
    }

    private Map<String, Object> response(WithdrawRequest request) {
        return Map.of("requestNo", request.getRequestNo(), "status", request.getRequestStatus(), "amount", request.getRequestedDiamondAmount());
    }
}
