package com.elice.cinema.domain.refund.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminRefundResponse {

    private Long refundId;

    private Integer refundAmount;
    private Integer refundRate;

    private String policyName;

    private LocalDateTime refundedAt;
}
