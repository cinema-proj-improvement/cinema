package com.elice.cinema.domain.payment.mapper;

import com.elice.cinema.domain.member.entity.Member;
import com.elice.cinema.domain.payment.dto.response.MypagePaymentResponse;
import com.elice.cinema.domain.payment.dto.response.TossConfirmResponse;
import com.elice.cinema.domain.payment.entity.Payment;
import com.elice.cinema.domain.payment.entity.PaymentStatus;
import com.elice.cinema.domain.reservation.entity.Reservation;
import org.mapstruct.Mapper;

import java.time.OffsetDateTime;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    default Payment toEntity(TossConfirmResponse res,
                             Reservation reservation,
                             Member member) {
        return Payment.of(
                reservation,
                member,
                res.getOrderId(), // orderId로 받아서 reservationCode에 넣기
                res.getPaymentKey(),
                res.getTotalAmount(),
                PaymentStatus.PAID,
                OffsetDateTime.parse(res.getApprovedAt()),
                res.getMethod()
        );
    }

    MypagePaymentResponse toMypagePaymentResponse(Payment payment);
}
