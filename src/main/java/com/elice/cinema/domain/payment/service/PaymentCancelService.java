package com.elice.cinema.domain.payment.service;

import com.elice.cinema.domain.payment.dto.response.TossCancelResponse;
import com.elice.cinema.domain.payment.entity.Payment;
import com.elice.cinema.domain.payment.repository.PaymentRepository;
import com.elice.cinema.domain.policy.dto.response.RefundCalculationResult;
import com.elice.cinema.domain.policy.service.RefundPolicyService;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 결제 취소 공통 유스케이스
 * <p>
 * - 관리자 / 사용자 취소의 공통 진입점
 * - PG 취소, 상태 전이, 환불 생성은 이 서비스에 수렴시킨다
 * <p>
 * 현재 단계:
 * - 실제 구현은 AdminPaymentService / PaymentService에 분산되어 있음
 * - 본 클래스는 "취소 유스케이스의 중심"을 고정하기 위한 최소 형태
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCancelService {

    private final TossPaymentsClient tossPaymentsClient;
    private final PaymentTxService paymentTxService;
    private final RefundPolicyService refundPolicyService;
    private final PaymentRepository paymentRepository;

    public void cancel(Long paymentId) {
        Payment payment = paymentRepository.findByIdWithReservationAndScreening(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        // 환불 정책 계산
        RefundCalculationResult result =
                refundPolicyService.calculate(payment, LocalDateTime.now());

        if (!result.isRefundable()) {
            throw new BusinessException(ErrorCode.REFUND_NOT_ALLOWED
            );
        }

        boolean pgCanceled = tryPgCancelWithRetry(payment.getPaymentKey(), result.getCancelAmount(), result.getReason(), 3);

        if (!pgCanceled) {
            // 3번 다 실패 => DB에 CANCEL_FAILED 기록
            paymentTxService.commitCancelFailed(paymentId, result);
            throw new BusinessException(ErrorCode.PAYMENT_CANCEL_FAILED);
        }

        // DB 기록
        paymentTxService.commitCancelSuccess(paymentId, result);
    }

    // 결제 취소 retry 로직
    public boolean tryPgCancelWithRetry(String paymentKey, long cancelAmount, String reason, int maxAttempts) {
        RuntimeException lastEx = null;


        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                TossCancelResponse res = tossPaymentsClient.tossCancel(
                        paymentKey,
                        cancelAmount,
                        reason
                );

                boolean statusOk = "CANCELED".equals(res.getStatus()) || "PARTIAL_CANCELED".equals(res.getStatus());
                boolean amountOk = cancelAmount == res.getLatestCancelAmount();

                if (statusOk && amountOk)
                    return true;
            } catch (RuntimeException ex) {
                lastEx = ex;
                // 마지막 시도면 종료
                if (attempt == maxAttempts) break;

                // (선택) 간단 백오프: 300ms -> 600ms
                sleepSilently(300L * attempt);
            }
        }
        // 취소 실패 에러 로그
        log.warn("PG cancel failed after {} attempts., paymentKey={}",
                maxAttempts, paymentKey, lastEx);
        return false;
    }

    private void sleepSilently(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
