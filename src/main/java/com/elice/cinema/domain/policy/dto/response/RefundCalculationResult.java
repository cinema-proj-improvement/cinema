package com.elice.cinema.domain.policy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RefundCalculationResult {

    private final boolean refundable;
    private final Long cancelAmount;
    private final int refundRate;
    private final String policyName;
    private final String reason;

    public static RefundCalculationResult refundable(
            long cancelAmount,
            int refundRate,
            String policyName
    ) {
        return new RefundCalculationResult(
                true,
                cancelAmount,
                refundRate,
                policyName,
                null
        );
    }

    public static RefundCalculationResult notRefundable(String reason) {
        return new RefundCalculationResult(
                false,
                0L,
                0,
                null,
                reason
        );
    }
}