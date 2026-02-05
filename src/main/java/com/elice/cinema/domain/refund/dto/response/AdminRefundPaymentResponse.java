package com.elice.cinema.domain.refund.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminRefundPaymentResponse {
    private Long paymentId;
    private String reservationCode;
}
