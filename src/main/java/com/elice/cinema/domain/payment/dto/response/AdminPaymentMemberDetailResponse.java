package com.elice.cinema.domain.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminPaymentMemberDetailResponse {
    private Long memberId;
    private String name;
    private String email;
}