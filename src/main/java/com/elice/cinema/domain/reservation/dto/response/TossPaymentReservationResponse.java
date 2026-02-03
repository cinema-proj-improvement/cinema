package com.elice.cinema.domain.reservation.dto.response;

import com.elice.cinema.domain.member.dto.PaymentMemberResponse;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TossPaymentReservationResponse {
    private String orderId;
    private String movieTitle;
    private Integer totalPrice;
    private String tossClientKey;
    private PaymentMemberResponse member;
}
