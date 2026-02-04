package com.elice.cinema.domain.payment.dto.response;

import lombok.Getter;

@Getter
public class TossConfirmResponse {
    private String paymentKey;
    private String orderId;
    private String status;
    private Long totalAmount;
    private String method;
    private String approvedAt;
}
