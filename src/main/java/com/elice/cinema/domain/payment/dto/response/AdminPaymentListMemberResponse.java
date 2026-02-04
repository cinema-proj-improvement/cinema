package com.elice.cinema.domain.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminPaymentListMemberResponse {
    private String name;
    private String email;
}
