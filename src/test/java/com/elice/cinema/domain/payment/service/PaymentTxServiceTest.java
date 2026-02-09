package com.elice.cinema.domain.payment.service;

import com.elice.cinema.domain.member.entity.Member;
import com.elice.cinema.domain.member.repository.MemberRepository;
import com.elice.cinema.domain.payment.dto.response.TossConfirmResponse;
import com.elice.cinema.domain.payment.entity.Payment;
import com.elice.cinema.domain.payment.entity.PaymentStatus;
import com.elice.cinema.domain.payment.mapper.PaymentMapper;
import com.elice.cinema.domain.payment.repository.PaymentRepository;
import com.elice.cinema.domain.policy.dto.response.RefundCalculationResult;
import com.elice.cinema.domain.refund.service.RefundService;
import com.elice.cinema.domain.reservation.entity.Reservation;
import com.elice.cinema.domain.reservation.entity.ReservedSeat;
import com.elice.cinema.domain.reservation.repository.ReservationRepository;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentTxServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock ReservationRepository reservationRepository;
    @Mock MemberRepository memberRepository;
    @Mock PaymentMapper paymentMapper;
    @Mock RefundService refundService;

    @InjectMocks PaymentTxService paymentTxService;

    @Test
    @DisplayName("멱등: paymentKey 이미 존재하면 바로 return(저장/상태변경 X)")
    void commitPaymentSuccess_idempotent() {
        // given
        TossConfirmResponse res = mock(TossConfirmResponse.class);
        when(res.getPaymentKey()).thenReturn("payKey");
        when(paymentRepository.existsByPaymentKey("payKey")).thenReturn(true);

        // when
        paymentTxService.commitPaymentSuccess(res, 1L, 1L);

        // then
        verify(reservationRepository, never()).findWithReservedSeatsById(anyLong());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("성공: 금액 일치 → reservation.confirm + reservedSeat.confirm + payment save")
    void commitPaymentSuccess_success_flow() {
        // given
        Long reservationId = 10L;
        Long memberId = 20L;

        TossConfirmResponse res = mock(TossConfirmResponse.class);
        when(res.getPaymentKey()).thenReturn("payKey-10");
        when(res.getTotalAmount()).thenReturn(15000L);

        when(paymentRepository.existsByPaymentKey("payKey-10")).thenReturn(false);

        Reservation reservation = mock(Reservation.class);
        when(reservation.getTotalPrice()).thenReturn(15000);

        ReservedSeat seat1 = mock(ReservedSeat.class);
        ReservedSeat seat2 = mock(ReservedSeat.class);
        when(reservation.getReservedSeats()).thenReturn(List.of(seat1, seat2));

        when(reservationRepository.findWithReservedSeatsById(reservationId))
                .thenReturn(Optional.of(reservation));

        Member member = mock(Member.class);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        Payment payment = mock(Payment.class);
        when(paymentMapper.toEntity(res, reservation, member)).thenReturn(payment);

        // when
        paymentTxService.commitPaymentSuccess(res, reservationId, memberId);

        // then
        verify(reservation, times(1)).confirm();
        verify(seat1, times(1)).confirm();
        verify(seat2, times(1)).confirm();
        verify(paymentRepository, times(1)).save(payment);
    }

    @Test
    @DisplayName("실패: 승인 금액 != 예약 금액 → PAYMENT_AMOUNT_MISMATCH")
    void commitPaymentSuccess_amount_mismatch() {
        // given
        TossConfirmResponse res = mock(TossConfirmResponse.class);
        when(res.getPaymentKey()).thenReturn("payKey-x");
        when(res.getTotalAmount()).thenReturn(15000L);

        when(paymentRepository.existsByPaymentKey("payKey-x")).thenReturn(false);

        Reservation reservation = mock(Reservation.class);
        when(reservation.getTotalPrice()).thenReturn(14000);

        when(reservation.getReservedSeats()).thenReturn(List.of(mock(ReservedSeat.class)));

        when(reservationRepository.findWithReservedSeatsById(1L))
                .thenReturn(Optional.of(reservation));

        when(memberRepository.findById(1L)).thenReturn(Optional.of(mock(Member.class)));

        // when & then
        BusinessException ex = catchThrowableOfType(
                () -> paymentTxService.commitPaymentSuccess(res, 1L, 1L),
                BusinessException.class
        );

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        verify(paymentRepository, never()).save(any());
        verify(reservation, never()).confirm();
    }

    @Test
    @DisplayName("롤백 취소성공 기록: markCanceled + reservation.fail + save")
    void commitRollbackCanceled_marks_and_saves() {
        // given
        TossConfirmResponse res = mock(TossConfirmResponse.class);
        when(res.getPaymentKey()).thenReturn("payKey-rb");
        // findByPaymentKey 경로로 들어가면 mapper/회원/예약 로딩이 필요없음
        Payment payment = mock(Payment.class);
        when(paymentRepository.findByPaymentKey("payKey-rb")).thenReturn(Optional.of(payment));
        when(payment.getStatus()).thenReturn(PaymentStatus.PAID); // CANCELED 아니면 진행

        Reservation reservation = mock(Reservation.class);
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        // when
        paymentTxService.commitRollbackCanceled(res, 100L, 200L, "fail msg");

        // then
        verify(payment, times(1)).markCanceled("fail msg");
        verify(reservation, times(1)).fail();
        verify(paymentRepository, times(1)).save(payment);
    }

    @Test
    @DisplayName("취소 성공 커밋: reservation.cancel + markCanceled + refundService.createRefund")
    void commitCancelSuccess_calls_refund() {
        // given
        Long paymentId = 77L;

        Payment payment = mock(Payment.class);
        Reservation reservation = mock(Reservation.class);
        when(payment.getReservation()).thenReturn(reservation);
        when(payment.getStatus()).thenReturn(PaymentStatus.PAID);

        when(paymentRepository.findByIdWithReservation(paymentId)).thenReturn(Optional.of(payment));

        RefundCalculationResult result = mock(RefundCalculationResult.class);
        when(result.getReason()).thenReturn("정책상 취소");
        when(result.getCancelAmount()).thenReturn(12000L);

        // when
        paymentTxService.commitCancelSuccess(paymentId, result);

        // then
        verify(reservation, times(1)).cancel();
        verify(payment, times(1)).markCanceled("정책상 취소");
        verify(refundService, times(1)).createRefund(payment, 12000L);
    }
}
