package com.elice.cinema.domain.payment.service;

import com.elice.cinema.domain.payment.dto.request.AdminPaymentSearchCondition;
import com.elice.cinema.domain.payment.dto.response.AdminPaymentDetailResponse;
import com.elice.cinema.domain.payment.dto.response.AdminPaymentListResponse;
import com.elice.cinema.domain.payment.entity.Payment;
import com.elice.cinema.domain.payment.repository.PaymentRepository;
import com.elice.cinema.domain.refund.service.RefundService;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPaymentService {

    private final PaymentRepository paymentRepository;
    private final RefundService refundService;
    private final PaymentCancelService paymentCancelService;

    // 결제 목록 조회
    public Page<AdminPaymentListResponse> getAdminPaymentList(
            AdminPaymentSearchCondition condition,
            Pageable pageable
    ) {
        condition.applyDefaultDateIfEmpty();
        return paymentRepository.findPayments(condition, pageable);
    }

    // 결제 상세 조회
    public AdminPaymentDetailResponse getAdminPaymentDetail(Long paymentId) {

        return paymentRepository.findAdminPaymentDetailById(paymentId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.PAYMENT_NOT_FOUND)
                );
    }

    public void cancelByAdmin(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        paymentCancelService.cancel(payment);
    }

}

