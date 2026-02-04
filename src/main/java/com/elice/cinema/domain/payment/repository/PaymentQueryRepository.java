package com.elice.cinema.domain.payment.repository;

import com.elice.cinema.domain.payment.dto.request.AdminPaymentSearchCondition;
import com.elice.cinema.domain.payment.dto.response.AdminPaymentListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentQueryRepository {
    Page<AdminPaymentListResponse> findPayments(AdminPaymentSearchCondition condition, Pageable pageable);
}
