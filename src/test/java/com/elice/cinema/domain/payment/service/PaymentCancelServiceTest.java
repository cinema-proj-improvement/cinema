package com.elice.cinema.domain.payment.service;

import com.elice.cinema.domain.payment.entity.Payment;
import com.elice.cinema.domain.payment.repository.PaymentRepository;
import com.elice.cinema.domain.policy.dto.response.RefundCalculationResult;
import com.elice.cinema.domain.policy.service.RefundPolicyService;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentCancelServiceTest {

    @Mock PaymentTxService paymentTxService;
    @Mock RefundPolicyService refundPolicyService;
    @Mock PaymentRepository paymentRepository;

    @InjectMocks PaymentCancelService paymentCancelService;

    @Test
    @DisplayName("취소 실패(정책): refundable=false → REFUND_NOT_ALLOWED")
    void cancel_not_refundable_then_throw() {
        // given
        Long paymentId = 1L;
        Payment payment = mock(Payment.class);

        when(paymentRepository.findByIdWithReservationAndScreening(paymentId))
                .thenReturn(Optional.of(payment));

        RefundCalculationResult result = mock(RefundCalculationResult.class);
        when(result.isRefundable()).thenReturn(false);

        when(refundPolicyService.calculate(eq(payment), any(LocalDateTime.class)))
                .thenReturn(result);

        // when & then
        BusinessException ex = catchThrowableOfType(
                () -> paymentCancelService.cancel(paymentId),
                BusinessException.class
        );

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REFUND_NOT_ALLOWED);

        verify(paymentTxService, never()).commitCancelFailed(anyLong(), any());
        verify(paymentTxService, never()).commitCancelSuccess(anyLong(), any());
    }

    @Test
    @DisplayName("취소 실패(PG): tryPgCancelWithRetry=false → commitCancelFailed 후 PAYMENT_CANCEL_FAILED")
    void cancel_pg_failed_then_commit_failed() {
        // given
        Long paymentId = 2L;
        Payment payment = mock(Payment.class);

        when(paymentRepository.findByIdWithReservationAndScreening(paymentId))
                .thenReturn(Optional.of(payment));

        RefundCalculationResult result = mock(RefundCalculationResult.class);
        when(result.isRefundable()).thenReturn(true);
        when(result.getCancelAmount()).thenReturn(15000L);
        when(result.getReason()).thenReturn("테스트 취소");

        when(payment.getPaymentKey()).thenReturn("payKey-2");

        when(refundPolicyService.calculate(eq(payment), any(LocalDateTime.class)))
                .thenReturn(result);

        // Spy로 tryPgCancelWithRetry만 false로 만들기
        PaymentCancelService spy = spy(paymentCancelService);
        doReturn(false).when(spy).tryPgCancelWithRetry(eq("payKey-2"), eq(15000L), eq("테스트 취소"), eq(3));

        // when & then
        BusinessException ex = catchThrowableOfType(
                () -> spy.cancel(paymentId),
                BusinessException.class
        );

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_CANCEL_FAILED);

        verify(paymentTxService, times(1)).commitCancelFailed(paymentId, result);
        verify(paymentTxService, never()).commitCancelSuccess(anyLong(), any());
    }

    @Test
    @DisplayName("취소 성공: tryPgCancelWithRetry=true → commitCancelSuccess 호출")
    void cancel_pg_success_then_commit_success() {
        // given
        Long paymentId = 3L;
        Payment payment = mock(Payment.class);

        when(paymentRepository.findByIdWithReservationAndScreening(paymentId))
                .thenReturn(Optional.of(payment));

        RefundCalculationResult result = mock(RefundCalculationResult.class);
        when(result.isRefundable()).thenReturn(true);
        when(result.getCancelAmount()).thenReturn(15000L);
        when(result.getReason()).thenReturn("테스트 취소");

        when(payment.getPaymentKey()).thenReturn("payKey-3");

        when(refundPolicyService.calculate(eq(payment), any(LocalDateTime.class)))
                .thenReturn(result);

        PaymentCancelService spy = spy(paymentCancelService);
        doReturn(true).when(spy).tryPgCancelWithRetry(eq("payKey-3"), eq(15000L), eq("테스트 취소"), eq(3));

        // when
        spy.cancel(paymentId);

        // then
        verify(paymentTxService, times(1)).commitCancelSuccess(paymentId, result);
        verify(paymentTxService, never()).commitCancelFailed(anyLong(), any());
    }
}
