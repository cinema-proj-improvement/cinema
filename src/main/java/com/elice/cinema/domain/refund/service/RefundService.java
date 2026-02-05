package com.elice.cinema.domain.refund.service;

import com.elice.cinema.domain.payment.entity.Payment;
import com.elice.cinema.domain.policy.entity.RefundPolicy;
import com.elice.cinema.domain.policy.repository.EnvironmentPolicyRepository;
import com.elice.cinema.domain.policy.repository.RefundPolicyRepository;
import com.elice.cinema.domain.refund.dto.response.AdminRefundHistoryListResponse;
import com.elice.cinema.domain.refund.entity.Refund;
import com.elice.cinema.domain.refund.repository.RefundRepository;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

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
    public Refund createRefund(Payment payment, Long cancelAmount) {
        Refund refund = Refund.create(payment, cancelAmount);

        return refundRepository.save(refund);
    }

    /**
     * 환불 정책 선택
     *
     * 규칙:
     * - beforeStartMinutes >= 남은 시간
     * - 조건 만족 정책 중 가장 작은 beforeStartMinutes
     */
    private RefundPolicy selectRefundPolicy(int minutesBeforeStart) {
        RefundPolicy policy = refundPolicyRepository
                .findFirstByBeforeStartMinutesLessThanEqualOrderByBeforeStartMinutesDesc(
                        minutesBeforeStart
                )
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.REFUND_POLICY_NOT_FOUND)
                );

        return policy;
    }

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

    /**
     * 관리자 환불 이력 조회
     */
    public Page<AdminRefundHistoryListResponse> findAdminRefundHistories(
            LocalDate from,
            LocalDate to,
            String keyword,
            Pageable pageable
    ) {
        return refundRepository.findAdminRefundHistories(
                from == null ? null : from.atStartOfDay(),
                to == null ? null : to.atTime(23, 59, 59),
                (keyword == null || keyword.isBlank()) ? null : keyword,
                pageable
        );
    }
}