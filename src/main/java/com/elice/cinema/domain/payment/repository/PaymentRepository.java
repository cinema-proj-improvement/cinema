package com.elice.cinema.domain.payment.repository;

import com.elice.cinema.domain.payment.dto.response.AdminPaymentDetailResponse;
import com.elice.cinema.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long>, PaymentQueryRepository {
    boolean existsByPaymentKey(String paymentKey);

    Optional<Payment> findByReservationId(Long reservationId);
    Optional<Payment> findByPaymentKey(String paymentKey);

    @Query("""
    select p from Payment p
    join fetch p.reservation r
    where p.id = :paymentId
""")
    Optional<Payment> findByIdWithReservation(@Param("paymentId") Long paymentId);

    @Query("""
        select new com.elice.cinema.domain.payment.dto.response.AdminPaymentDetailResponse(
            p.id,
            p.reservationCode,
            p.amount,
            p.status,
            p.approvedAt,
            p.method,
            p.paymentKey,
            p.failureMessage,
            new com.elice.cinema.domain.payment.dto.response.AdminPaymentMemberDetailResponse(
                m.id, m.name, m.email
            ),
            new com.elice.cinema.domain.payment.dto.response.AdminPaymentReservationDetailResponse(
                r.id, r.reservationCode, mv.title, sc.name, s.startAt, s.id
            )
        )
        from Payment p
        join p.member m
        join p.reservation r
        join r.screening s
        join s.movie mv
        join s.screen sc
        where p.id = :paymentId
    """)
    Optional<AdminPaymentDetailResponse> findAdminPaymentDetailById(@Param("paymentId") Long paymentId);

    @Query("""
        select p
        from Payment p
        join fetch p.reservation r
        left join fetch r.member m
        left join fetch r.screening sc
        where p.id = :paymentId
    """)
    Optional<Payment> findByIdWithReservationAndScreening(@Param("paymentId") Long paymentId);
}
