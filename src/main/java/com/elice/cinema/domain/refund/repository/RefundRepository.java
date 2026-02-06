package com.elice.cinema.domain.refund.repository;

import com.elice.cinema.domain.refund.entity.Refund;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface RefundRepository extends JpaRepository<Refund, Long> {
    @Query("""
        select r
        from Refund r
        join r.payment p
        join p.reservation res
        where (:from is null or r.refundedAt >= :from)
          and (:toExclusive is null or r.refundedAt < :toExclusive)
          and (
                :keyword is null
             or :keyword = ''
             or lower(res.reservationCode) like lower(concat('%', :keyword, '%'))
             or lower(res.memberName) like lower(concat('%', :keyword, '%'))
          )
        order by r.refundedAt desc
    """)
    Page<Refund> searchAdminRefunds(
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}