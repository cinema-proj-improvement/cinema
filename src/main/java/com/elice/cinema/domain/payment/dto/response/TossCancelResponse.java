package com.elice.cinema.domain.payment.dto.response;

import lombok.Getter;

@Getter
public class TossCancelResponse {
    private String paymentKey;
    private String orderId;
    private String status;
}
