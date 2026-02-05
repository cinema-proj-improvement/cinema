package com.elice.cinema.domain.refund.repository;

import com.elice.cinema.domain.refund.dto.response.AdminRefundHistoryListResponse;
import com.elice.cinema.domain.refund.entity.Refund;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    Optional<Refund> findByPaymentId(Long paymentId);

    boolean existsByPaymentId(Long paymentId);

    @Query("""
        select new com.elice.cinema.domain.refund.dto.response.AdminRefundHistoryListResponse(
            new com.elice.cinema.domain.refund.dto.response.AdminRefundResponse(
                r.id,
                r.refundAmount,
                r.refundRate,
                r.policyName,
                r.refundedAt
            ),
            new com.elice.cinema.domain.refund.dto.response.AdminRefundPaymentResponse(
                p.id,
                p.reservationCode
            ),
            new com.elice.cinema.domain.refund.dto.response.AdminRefundMemberResponse(
                m.email
            )
        )
        from Refund r
        join r.payment p
        join p.reservation res
        join res.member m
        where (:from is null or r.refundedAt >= :from)
          and (:to is null or r.refundedAt <= :to)
          and (
                :keyword is null or
                m.email like concat('%', :keyword, '%') or
                p.reservationCode like concat('%', :keyword, '%')
          )
        order by r.refundedAt desc
    """)
    Page<AdminRefundHistoryListResponse> findAdminRefundHistories(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}