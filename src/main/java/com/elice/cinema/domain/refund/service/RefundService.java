package com.elice.cinema.domain.refund.service;

import com.elice.cinema.domain.payment.entity.Payment;
import com.elice.cinema.domain.policy.repository.EnvironmentPolicyRepository;
import com.elice.cinema.domain.policy.repository.RefundPolicyRepository;
import com.elice.cinema.domain.refund.entity.Refund;
import com.elice.cinema.domain.refund.repository.RefundRepository;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefundService {

    private final RefundRepository refundRepository;
    private final RefundPolicyRepository refundPolicyRepository;
    private final EnvironmentPolicyRepository environmentPolicyRepository;


    /**
     * 결제 기준 환불 생성
     */
    @Transactional
    public void createRefund(Payment payment, Long cancelAmount) {
        Refund refund = Refund.create(payment, cancelAmount);
        refundRepository.save(refund);
    }

    /**
     * 환불 정책 선택
     *
     * 규칙:
     * - beforeStartMinutes >= 남은 시간
     * - 조건 만족 정책 중 가장 작은 beforeStartMinutes
     */
    /*private RefundPolicy selectRefundPolicy(int minutesBeforeStart) {
        RefundPolicy policy = refundPolicyRepository
                .findFirstByBeforeStartMinutesLessThanEqualOrderByBeforeStartMinutesDesc(
                        minutesBeforeStart
                )
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.REFUND_POLICY_NOT_FOUND)
                );

        return policy;
    }*/

    /**
     * 환불 금액 계산
     */
    private int calculateRefundAmount(Long paymentAmount, int refundRate) {

        if (paymentAmount == null || paymentAmount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }

        if (refundRate < 0 || refundRate > 100) {
            throw new BusinessException(ErrorCode.INVALID_REFUND_RATE);
        }

        return Math.toIntExact(paymentAmount * refundRate / 100);
    }


}