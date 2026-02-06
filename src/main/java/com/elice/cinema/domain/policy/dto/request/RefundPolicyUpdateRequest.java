package com.elice.cinema.domain.policy.dto.request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefundPolicyUpdateRequest {

    private Integer beforeStartMinutes;
    private Integer refundRate;
}
