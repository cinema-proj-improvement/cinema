package com.elice.cinema.domain.payment.dto.response;

import com.elice.cinema.domain.payment.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@AllArgsConstructor
public class AdminPaymentDetailResponse {

    private Long id;
    private String reservationCode;
    private Long amount;
    private PaymentStatus status;
    private OffsetDateTime approvedAt;
    private String method;
    private String paymentKey;
    private String failureMessage;

    private AdminPaymentMemberDetailResponse member;
    private AdminPaymentReservationDetailResponse reservation;

}