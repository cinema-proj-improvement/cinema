package com.elice.cinema.domain.payment.repository;

import com.elice.cinema.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
