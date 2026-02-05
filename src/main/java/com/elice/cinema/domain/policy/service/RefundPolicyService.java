package com.elice.cinema.domain.policy.service;

import com.elice.cinema.domain.payment.entity.Payment;
import com.elice.cinema.domain.policy.dto.response.RefundCalculationResult;
import com.elice.cinema.domain.policy.entity.RefundPolicy;
import com.elice.cinema.domain.policy.repository.RefundPolicyRepository;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefundPolicyService {

    private final RefundPolicyRepository refundPolicyRepository;
    private final EnvironmentPolicyService environmentPolicyService;

    public List<RefundPolicy> findAll() {
        return refundPolicyRepository.findAll(
                Sort.by(Sort.Direction.DESC, "beforeStartMinutes")
        );
    }

    @Transactional
    public RefundPolicy create(String name, Integer beforeStartMinutes, Integer refundRate) {
        if (refundPolicyRepository.existsByBeforeStartMinutes(beforeStartMinutes)) {
            throw new BusinessException(ErrorCode.REFUND_POLICY_DUPLICATED_BEFORE_TIME);
        }

        return refundPolicyRepository.save(
                RefundPolicy.create(name, beforeStartMinutes, refundRate)
        );
    }

    @Transactional
    public void update(Long policyId, Integer beforeStartMinutes, Integer refundRate) {
        RefundPolicy policy = refundPolicyRepository.findById(policyId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.REFUND_POLICY_NOT_FOUND)
                );

        if (!policy.getBeforeStartMinutes().equals(beforeStartMinutes)
                && refundPolicyRepository.existsByBeforeStartMinutes(beforeStartMinutes)) {
            throw new BusinessException(ErrorCode.REFUND_POLICY_DUPLICATED_BEFORE_TIME);
        }

        policy.update(beforeStartMinutes, refundRate);
    }

    public RefundCalculationResult calculate(
            Payment payment,
            LocalDateTime now
    ) {
        LocalDateTime screeningStart =
                payment.getReservation().getScreening().getStartAt();

        long minutesBeforeStart =
                Duration.between(now, screeningStart).toMinutes();

        if (minutesBeforeStart < 0) {
            return RefundCalculationResult.notRefundable("상영 시작 이후");
        }


        if (minutesBeforeStart < environmentPolicyService.getRefundDeadlineMinutes()) {
            return RefundCalculationResult.notRefundable("환불 데드라인 초과");
        }

        RefundPolicy policy = refundPolicyRepository.
                findFirstByBeforeStartMinutesLessThanEqualOrderByBeforeStartMinutesDesc(minutesBeforeStart)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFUND_POLICY_NOT_FOUND));

        long cancelAmount =
                payment.getAmount() * policy.getRefundRate() / 100;

        return RefundCalculationResult.refundable(
                cancelAmount,
                policy.getRefundRate(),
                policy.getName()
        );
    }
}
