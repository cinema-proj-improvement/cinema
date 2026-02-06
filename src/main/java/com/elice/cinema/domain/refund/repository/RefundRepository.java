package com.elice.cinema.domain.refund.repository;

import com.elice.cinema.domain.refund.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<Refund, Long> {
}