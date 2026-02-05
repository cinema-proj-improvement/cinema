package com.elice.cinema.domain.refund.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminRefundHistoryListResponse {
    private AdminRefundResponse refund;
    private AdminRefundPaymentResponse payment;
    private AdminRefundMemberResponse member;
}
