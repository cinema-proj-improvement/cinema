package com.elice.cinema.domain.payment.service;

import com.elice.cinema.domain.payment.dto.request.AdminPaymentSearchCondition;
import com.elice.cinema.domain.payment.dto.response.AdminPaymentDetailResponse;
import com.elice.cinema.domain.payment.dto.response.AdminPaymentListResponse;
import com.elice.cinema.domain.payment.dto.response.AdminPaymentReservationDetailResponse;
import com.elice.cinema.domain.payment.repository.PaymentRepository;
import com.elice.cinema.domain.reservation.repository.ReservedSeatRepository;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservedSeatRepository reservedSeatRepository;

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

        AdminPaymentDetailResponse base =
                paymentRepository.findAdminPaymentDetailById(paymentId)
                        .orElseThrow(() ->
                                new BusinessException(ErrorCode.PAYMENT_NOT_FOUND)
                        );

        // 좌석 조회 (단건 → N+1 아님)
        List<String> seatCodes = reservedSeatRepository.findSeatCodesByReservationId(
                base.getReservation().getReservationId()
        );

        // reservation DTO 재조립
        AdminPaymentReservationDetailResponse r = base.getReservation();
        AdminPaymentReservationDetailResponse withSeats = new AdminPaymentReservationDetailResponse(
                r.getReservationId(),
                r.getReservationCode(),
                r.getMovieTitle(),
                r.getScreenName(),
                r.getStartAt(),
                r.getScreeningId(),
                seatCodes
        );

        return new AdminPaymentDetailResponse(
                base.getId(),
                base.getReservationCode(),
                base.getAmount(),
                base.getStatus(),
                base.getApprovedAt(),
                base.getMethod(),
                base.getPaymentKey(),
                base.getFailureMessage(),
                base.getMember(),
                withSeats
        );
    }
}

