package com.elice.cinema.domain.payment.service;

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
 *
 * - 관리자 / 사용자 취소의 공통 진입점
 * - PG 취소, 상태 전이, 환불 생성은 이 서비스에 수렴시킨다
 *
 * 현재 단계:
 * - 실제 구현은 AdminPaymentService / PaymentService에 분산되어 있음
 * - 본 클래스는 "취소 유스케이스의 중심"을 고정하기 위한 최소 형태
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCancelService {

    private final PaymentService paymentService;
    private final PaymentTxService paymentTxService;
    private final RefundPolicyService refundPolicyService;
    private final PaymentRepository paymentRepository;

    public void cancel(Long paymentId) {
        Payment payment = paymentRepository.findByIdWithReservationAndScreening(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        // 1️⃣ 환불 정책 계산 (트랜잭션 ❌)
        RefundCalculationResult result =
                refundPolicyService.calculate(payment, LocalDateTime.now());

        if (!result.isRefundable()) {
            throw new BusinessException(ErrorCode.REFUND_NOT_ALLOWED
            );
        }

        // 2️⃣ PG cancel (트랜잭션 ❌)
        paymentService.cancelPartially( // TODO: 추후 롤백, Retry 처리 추가
                payment.getPaymentKey(),
                result.getCancelAmount(),
                result.getPolicyName()
        );

        // 3️⃣ DB 기록 (트랜잭션 ⭕)
        paymentTxService.recordCancel(paymentId, payment.getReservation().getId(),result);
    }
}
