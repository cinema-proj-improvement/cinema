package com.elice.cinema.domain.payment.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class MypagePaymentResponse {
    private Long amount;
    private OffsetDateTime approvedAt;
    private String method;
}
