package com.elice.cinema.domain.payment.repository;

import com.elice.cinema.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsByPaymentKey(String paymentKey);

    Optional<Payment> findByPaymentKey(String paymentKey);
}
