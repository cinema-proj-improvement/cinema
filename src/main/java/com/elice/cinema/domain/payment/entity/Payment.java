package com.elice.cinema.domain.payment.entity;

import com.elice.cinema.domain.member.entity.Member;
import com.elice.cinema.domain.reservation.entity.Reservation;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "payments")
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false) //TODO: 예매 건당 재결제 등 여러결제 고려시 ManyToOne 로 변경하기.
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "reservation_code", nullable=false, unique=true)
    private String reservationCode;

    @Column(name = "payment_key", nullable=false, unique=true)
    private String paymentKey;          // 토스 결제 식별자

    @Column(name = "amount", nullable=false)
    private Long amount;                // 결제 금액

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable=false)
    private PaymentStatus status;       // PAID, FAILED, CANCELED

    @Column(name = "approved_at", nullable=false)
    private OffsetDateTime approvedAt;   // 토스가 결제 승인한 시간

    @Column(name = "method", nullable = false)
    private String method;              // 사용하는 결제 수단

    @Column(name = "failure_message")
    private String failureMessage;

    private Payment(Reservation reservation,
                    Member member,
                    String reservationCode,
                    String paymentKey,
                    Long amount,
                    PaymentStatus status,
                    OffsetDateTime approvedAt,
                    String method) {
        this.reservation = reservation;
        this.member = member;
        this.reservationCode = reservationCode;
        this.paymentKey = paymentKey;
        this.amount = amount;
        this.status = status;
        this.approvedAt = approvedAt;
        this.method = method;
    }

    public static Payment of(Reservation reservation,
                             Member member,
                             String reservationCode,
                             String paymentKey,
                             Long amount,
                             PaymentStatus status,
                             OffsetDateTime approvedAt,
                             String method) {
        return new Payment(
                reservation,
                member,
                reservationCode,
                paymentKey,
                amount,
                status,
                approvedAt,
                method);
    }

    public void cancelPayment() {
        if (!this.status.canChangeTo(PaymentStatus.CANCELED)) {
            throw new BusinessException(ErrorCode.PAYMENT_CANCEL_NOT_ALLOWED);
        }
        this.status = PaymentStatus.CANCELED;
    }

    public void markCanceled(String reason) {
        if (!this.status.canChangeTo(PaymentStatus.CANCELED)) {
            throw new BusinessException(ErrorCode.PAYMENT_CANCEL_NOT_ALLOWED);
        }

        this.status = PaymentStatus.CANCELED;
        this.failureMessage = reason;
    }

    public void markCancelFailed(String reason) {
        // TODO: canChangeTo 사용해야하나 고민해보기
        this.status = PaymentStatus.CANCEL_FAILED;
        this.failureMessage = reason;
    }

    public boolean isCanceled() {
        return this.status == PaymentStatus.CANCELED;
    }
}
