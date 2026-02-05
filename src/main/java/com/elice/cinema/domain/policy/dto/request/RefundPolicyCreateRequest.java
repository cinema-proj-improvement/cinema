package com.elice.cinema.domain.policy.dto.request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefundPolicyCreateRequest {
    private String name;
    private Integer beforeStartMinutes;
    private Integer refundRate;
}
