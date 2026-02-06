package com.elice.cinema.domain.policy.entity;

import com.elice.cinema.global.common.audit.BaseEntity;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "refund_policies",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_refund_policy_before_start_minutes",
                        columnNames = "before_start_minutes"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefundPolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 정책명 */
    @Column(nullable = false, length = 50)
    private String name;

    /** 상영 시작까지 남은 시간 (분) */
    @Column(name = "before_start_minutes", nullable = false)
    private Integer beforeStartMinutes;

    /** 환불 퍼센트 (0~100) */
    @Column(name = "refund_rate", nullable = false)
    private Integer refundRate;

    private RefundPolicy(
            String name,
            Integer beforeStartMinutes,
            Integer refundRate
    ) {
        validate(name, beforeStartMinutes, refundRate);
        this.name = name;
        this.beforeStartMinutes = beforeStartMinutes;
        this.refundRate = refundRate;
    }

    public static RefundPolicy create(
            String name,
            Integer beforeStartMinutes,
            Integer refundRate
    ) {
        return new RefundPolicy(name, beforeStartMinutes, refundRate);
    }

    public void update(Integer beforeStartMinutes, Integer refundRate) {
        validate(this.name, beforeStartMinutes, refundRate);
        this.beforeStartMinutes = beforeStartMinutes;
        this.refundRate = refundRate;
    }

    private void validate(String name, Integer beforeStartMinutes, Integer refundRate) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.REFUND_POLICY_NAME_REQUIRED);
        }
        if (beforeStartMinutes == null || beforeStartMinutes < 0) {
            throw new BusinessException(ErrorCode.REFUND_POLICY_INVALID_BEFORE_TIME);
        }
        if (refundRate == null || refundRate < 0 || refundRate > 100) {
            throw new BusinessException(ErrorCode.REFUND_POLICY_INVALID_RATE);
        }
    }
}
