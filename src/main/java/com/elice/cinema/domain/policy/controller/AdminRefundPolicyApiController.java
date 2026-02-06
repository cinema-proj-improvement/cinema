package com.elice.cinema.domain.policy.controller;

import com.elice.cinema.domain.policy.dto.request.RefundPolicyCreateRequest;
import com.elice.cinema.domain.policy.dto.request.RefundPolicyUpdateRequest;
import com.elice.cinema.domain.policy.service.RefundPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/api/policies/refunds")
public class AdminRefundPolicyApiController {

    private final RefundPolicyService refundPolicyService;

    /**
     * 환불 정책 생성 (모달)
     */
    @PostMapping
    public ResponseEntity<Void> create(
            @RequestBody RefundPolicyCreateRequest request
    ) {
        refundPolicyService.create(
                request.getName(),
                request.getBeforeStartMinutes(),
                request.getRefundRate()
        );

        return ResponseEntity.ok().build();
    }

    /**
     * 환불 정책 수정 (리스트 inline edit)
     */
    @PutMapping("/{policyId}")
    public ResponseEntity<Void> update(
            @PathVariable Long policyId,
            @RequestBody RefundPolicyUpdateRequest request
    ) {
        refundPolicyService.update(
                policyId,
                request.getBeforeStartMinutes(),
                request.getRefundRate()
        );

        return ResponseEntity.ok().build();
    }
}
