package com.elice.cinema.domain.refund.entity;

import com.elice.cinema.domain.payment.entity.Payment;
import com.elice.cinema.global.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "refunds",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_refund_payment",
                        columnNames = "payment_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Refund extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 환불 대상 결제 */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    /** 환불 금액 */
    @Column(nullable = false)
    private Long refundAmount;

    /** 환불 결정 시각 */
    @Column(nullable = false)
    private LocalDateTime refundedAt;

    /* =========================
       생성자
    ========================= */

    private Refund(
            Payment payment,
            Long refundAmount,
            LocalDateTime refundedAt
    ) {
        this.payment = payment;
        this.refundAmount = refundAmount;
        this.refundedAt = refundedAt;
    }

    public static Refund create(
            Payment payment,
            Long refundAmount
    ) {
        return new Refund(
                payment,
                refundAmount,
                LocalDateTime.now()
        );
    }
}
