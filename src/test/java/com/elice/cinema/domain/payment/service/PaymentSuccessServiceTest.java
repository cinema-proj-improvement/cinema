package com.elice.cinema.domain.payment.service;

import com.elice.cinema.domain.member.entity.Member;
import com.elice.cinema.domain.member.repository.MemberRepository;
import com.elice.cinema.domain.payment.dto.response.TossConfirmResponse;
import com.elice.cinema.domain.payment.repository.PaymentRepository;
import com.elice.cinema.domain.reservation.entity.Reservation;
import com.elice.cinema.domain.reservation.entity.ReservationStatus;
import com.elice.cinema.domain.reservation.repository.ReservationRepository;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import com.elice.cinema.global.error.exception.PaymentFailRedirectException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentSuccessServiceTest {

    @Mock PaymentTxService paymentTxService;
    @Mock PaymentCancelService paymentCancelService;
    @Mock TossPaymentsClient tossPaymentsClient;
    @Mock PaymentRepository paymentRepository;
    @Mock ReservationRepository reservationRepository;
    @Mock MemberRepository memberRepository;

    @InjectMocks PaymentSuccessService paymentSuccessService;

    @Test
    @DisplayName("성공: 금액/본인 검증 OK + confirm DONE → commitPaymentSuccess 호출")
    void success_confirm_done_then_commit() {
        // given
        String paymentKey = "pay_test_001";
        String orderId = "R-20260215-002";
        Long amount = 15000L;
        Long memberId = 10L;
        Long reservationId = 99L;

        when(paymentRepository.existsByPaymentKey(paymentKey)).thenReturn(false);

        Reservation reservation = mock(Reservation.class);
        Member owner = mock(Member.class);

        when(reservationRepository.findByReservationCodeAndStatusWithMember(orderId, ReservationStatus.HOLD))
                .thenReturn(Optional.of(reservation));

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(mock(Member.class)));

        // validPayment 에서 호출됨
        when(reservation.getTotalPrice()).thenReturn(amount.intValue());
        when(reservation.getMember()).thenReturn(owner);
        when(owner.getId()).thenReturn(memberId);

        Member member = mock(Member.class);
        when(member.getId()).thenReturn(memberId);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        TossConfirmResponse confirmRes = mock(TossConfirmResponse.class);
        when(confirmRes.getStatus()).thenReturn("DONE");

        when(tossPaymentsClient.tossConfirm(paymentKey, orderId, amount)).thenReturn(confirmRes);

        // commit 인자로 들어감
        when(reservation.getId()).thenReturn(reservationId);

        // when
        paymentSuccessService.handleSuccess(paymentKey, orderId, amount, memberId);

        // then
        verify(paymentTxService, times(1))
                .commitPaymentSuccess(confirmRes, reservationId, memberId);

        // 성공이면 롤백 로직 호출 X
        verify(paymentCancelService, never())
                .tryPgCancelWithRetry(anyString(), anyLong(), anyString(), anyInt());
        verify(paymentTxService, never())
                .commitRollbackCanceled(any(), anyLong(), anyLong(), anyString());
        verify(paymentTxService, never())
                .commitRollbackCancelFailed(any(), anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("실패1: 금액 불일치 → PAYMENT_AMOUNT_MISMATCH, confirm 호출 X")
    void fail_amount_mismatch_before_confirm() {
        // given
        String paymentKey = "pay_test_002";
        String orderId = "R-20260215-003";
        Long requestAmount = 15000L;
        Long failAmount = 14000L;
        Long memberId = 10L;

        when(paymentRepository.existsByPaymentKey(paymentKey)).thenReturn(false);

        Reservation reservation = mock(Reservation.class);
        when(reservationRepository.findByReservationCodeAndStatusWithMember(orderId, ReservationStatus.HOLD))
                .thenReturn(Optional.of(reservation));

        when(memberRepository.findById(memberId))
                .thenReturn(Optional.of(mock(Member.class)));

        // validPayment의 "금액 검증"에서 바로 터지게 만들기
        when(reservation.getTotalPrice()).thenReturn(failAmount.intValue());

        // when & then
        BusinessException ex = catchThrowableOfType(
                () -> paymentSuccessService.handleSuccess(paymentKey, orderId, requestAmount, memberId),
                BusinessException.class
        );

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH);

        verify(tossPaymentsClient, never()).tossConfirm(anyString(), anyString(), anyLong());
        verify(paymentTxService, never()).commitPaymentSuccess(any(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("실패2: confirm status != DONE → PaymentFailRedirectException(PAYMENT_CONFIRM_FAILED)")
    void fail_confirm_not_done() {
        // given
        String paymentKey = "pay_test_003";
        String orderId = "R-20260215-004";
        Long amount = 15000L;
        Long memberId = 10L;

        when(paymentRepository.existsByPaymentKey(paymentKey)).thenReturn(false);

        Reservation reservation = mock(Reservation.class);
        Member owner = mock(Member.class);

        when(reservationRepository.findByReservationCodeAndStatusWithMember(orderId, ReservationStatus.HOLD))
                .thenReturn(Optional.of(reservation));

        Member member = mock(Member.class);
        when(member.getId()).thenReturn(memberId);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        when(reservation.getTotalPrice()).thenReturn(amount.intValue());
        when(reservation.getMember()).thenReturn(owner);
        when(owner.getId()).thenReturn(memberId);

        TossConfirmResponse confirmRes = mock(TossConfirmResponse.class);
        when(confirmRes.getStatus()).thenReturn("FAILED");

        when(tossPaymentsClient.tossConfirm(paymentKey, orderId, amount)).thenReturn(confirmRes);

        // when & then
        PaymentFailRedirectException ex = catchThrowableOfType(
                () -> paymentSuccessService.handleSuccess(paymentKey, orderId, amount, memberId),
                PaymentFailRedirectException.class
        );

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_CONFIRM_FAILED);

        verify(paymentTxService, never()).commitPaymentSuccess(any(), anyLong(), anyLong());
        verify(paymentCancelService, never()).tryPgCancelWithRetry(anyString(), anyLong(), anyString(), anyInt());
    }

    @Test
    @DisplayName("롤백1: confirm 이후 commitPaymentSuccess에서 BusinessException → PG 취소 성공 → rollbackCanceled + PAYMENT_CANCELED_AFTER_CONFIRM")
    void rollback_after_confirm_business_exception_cancel_success() {
        // given
        String paymentKey = "pay_test_rb_1";
        String orderId = "R-20260215-005";
        Long amount = 15000L;
        Long memberId = 10L;
        Long reservationId = 99L;

        when(paymentRepository.existsByPaymentKey(paymentKey)).thenReturn(false);

        Reservation reservation = mock(Reservation.class);
        Member owner = mock(Member.class);

        when(reservationRepository.findByReservationCodeAndStatusWithMember(orderId, ReservationStatus.HOLD))
                .thenReturn(Optional.of(reservation));

        when(reservation.getTotalPrice()).thenReturn(amount.intValue());
        when(reservation.getMember()).thenReturn(owner);
        when(owner.getId()).thenReturn(memberId);
        when(reservation.getId()).thenReturn(reservationId);

        Member member = mock(Member.class);
        when(member.getId()).thenReturn(memberId);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        TossConfirmResponse confirmRes = mock(TossConfirmResponse.class);
        when(confirmRes.getStatus()).thenReturn("DONE");
        when(confirmRes.getPaymentKey()).thenReturn(paymentKey);
        when(confirmRes.getTotalAmount()).thenReturn(amount);

        when(tossPaymentsClient.tossConfirm(paymentKey, orderId, amount)).thenReturn(confirmRes);

        // commit 단계에서 비즈니스 예외 발생
        doThrow(new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH))
                .when(paymentTxService).commitPaymentSuccess(confirmRes, reservationId, memberId);

        // PG 취소 성공
        when(paymentCancelService.tryPgCancelWithRetry(eq(paymentKey), eq(amount), anyString(), eq(3)))
                .thenReturn(true);

        // when & then
        BusinessException ex = catchThrowableOfType(
                () -> paymentSuccessService.handleSuccess(paymentKey, orderId, amount, memberId),
                BusinessException.class
        );

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_CANCELED_AFTER_CONFIRM);

        verify(paymentCancelService, times(1))
                .tryPgCancelWithRetry(eq(paymentKey), eq(amount), contains("결제 검증 실패"), eq(3));

        verify(paymentTxService, times(1))
                .commitRollbackCanceled(eq(confirmRes), eq(reservationId), eq(memberId), contains("rollback by business error"));

        verify(paymentTxService, never())
                .commitRollbackCancelFailed(any(), anyLong(), anyLong(), anyString());
    }
}
