package com.elice.cinema.domain.refund.service;

import com.elice.cinema.domain.payment.entity.Payment;
import com.elice.cinema.domain.refund.entity.Refund;
import com.elice.cinema.domain.refund.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefundService {

    private final RefundRepository refundRepository;

    @Transactional
    public void createRefund(Payment payment, Long cancelAmount) {
        Refund refund = Refund.create(payment, cancelAmount);
        refundRepository.save(refund);
    }
}