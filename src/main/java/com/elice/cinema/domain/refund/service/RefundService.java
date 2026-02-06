package com.elice.cinema.domain.refund.service;

import com.elice.cinema.domain.payment.entity.Payment;
import com.elice.cinema.domain.refund.dto.response.AdminRefundListResponse;
import com.elice.cinema.domain.refund.entity.Refund;
import com.elice.cinema.domain.refund.mapper.RefundMapper;
import com.elice.cinema.domain.refund.repository.RefundRepository;
import com.elice.cinema.domain.reservation.entity.Reservation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefundService {
    private final RefundRepository refundRepository;
    private final RefundMapper refundMapper;

    @Transactional
    public void createRefund(Payment payment, Long cancelAmount) {
        Refund refund = Refund.create(payment, cancelAmount);
        refundRepository.save(refund);
    }

    public Page<AdminRefundListResponse> searchAdminRefunds(
            LocalDate fromDate,
            LocalDate toDate,
            String keyword,
            Pageable pageable
    ) {
        LocalDateTime from = (fromDate == null) ? null : fromDate.atStartOfDay();
        LocalDateTime toExclusive = (toDate == null) ? null : toDate.plusDays(1).atStartOfDay();

        return refundRepository.searchAdminRefunds(from, toExclusive, keyword, pageable)
                .map(this::toAdminRefundListResponse);
    }

    private AdminRefundListResponse toAdminRefundListResponse(Refund refund) {
        Payment payment = refund.getPayment();
        Reservation reservation = payment.getReservation();

        return refundMapper.toAdminRefundListResponse(
                refund,
                reservation
        );
    }
}