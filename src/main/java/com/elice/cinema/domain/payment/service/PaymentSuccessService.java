package com.elice.cinema.domain.payment.service;

import com.elice.cinema.domain.member.entity.Member;
import com.elice.cinema.domain.member.repository.MemberRepository;
import com.elice.cinema.domain.payment.dto.response.TossConfirmResponse;
import com.elice.cinema.domain.payment.repository.PaymentRepository;
import com.elice.cinema.domain.reservation.entity.Reservation;
import com.elice.cinema.domain.reservation.entity.ReservationStatus;
import com.elice.cinema.domain.reservation.repository.ReservationRepository;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import com.elice.cinema.global.error.exception.PaymentFailRedirectException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentSuccessService {
    private final PaymentTxService paymentTxService;
    private final PaymentCancelService paymentCancelService;
    private final TossPaymentsClient tossPaymentsClient;
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;


    public void handleSuccess(final String paymentKey, final String orderId, final Long amount, final Long memberId) {
        //중복 호출 시 토스 confirm 호출 방지
        if (paymentRepository.existsByPaymentKey(paymentKey)) {
            return;
        }

        Reservation reservation = reservationRepository.findByReservationCodeAndStatusWithMember(orderId, ReservationStatus.HOLD)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        validPayment(reservation, amount, member);

        TossConfirmResponse confirmResponse = tossPaymentsClient.tossConfirm(paymentKey, orderId, amount);

        if (!"DONE".equals(confirmResponse.getStatus())) {
            throw new PaymentFailRedirectException(ErrorCode.PAYMENT_CONFIRM_FAILED, orderId);
        }

        try {
            paymentTxService.commitPaymentSuccess(confirmResponse, reservation.getId(), member.getId());
        } catch (BusinessException e) {
            rollbackByCancelOrThrow(confirmResponse, reservation.getId(), member.getId(),
                    "결제 검증 실패: " + e.getMessage(),
                    "rollback by business error: " + e.getErrorCode().name());
            throw e;
        } catch (RuntimeException e) {
            rollbackByCancelOrThrow(confirmResponse, reservation.getId(), member.getId(),
                    "서버 처리 실패",
                    "rollback by runtime error");
            throw e;
        }
    }

    private void validPayment(Reservation reservation, Long amount, Member member) {
        Long totalPrice = reservation.getTotalPrice().longValue();

        // 금액 검증
        if (!totalPrice.equals(amount)) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        // 본인 검증
        if (!reservation.getMember().getId().equals(member.getId())) {
            throw new BusinessException(ErrorCode.PAYMENT_FORBIDDEN);
        }
    }

    // 승인된 결제를 취소로 롤백하고, 취소 성공/실패를 DB에 남긴다.
    private void rollbackByCancelOrThrow(TossConfirmResponse confirmResponse,
                                         Long reservationId,
                                         Long memberId,
                                         String cancelReason,
                                         String failureMessage) {

        boolean canceled = paymentCancelService.tryPgCancelWithRetry(confirmResponse.getPaymentKey(), confirmResponse.getTotalAmount(), cancelReason, 3);

        // TODO: CONFIRM 이후 취소가 되어야 하는 상황에 맞는 취소 페이먼트 생성, 또는 취소를 시도하다 실패한 (취소해야하는) 취소 실패 결제도 추가
        // TODO: 이때는 모든 상태가 HOLD가 아닌 CANCELED가 되므로 결제 취소후 다시 예약 화면으로 보내는게 아닌 홈으로 보냄.
        if (canceled) {
            paymentTxService.commitRollbackCanceled(confirmResponse, reservationId, memberId, failureMessage);
            throw new BusinessException(ErrorCode.PAYMENT_CANCELED_AFTER_CONFIRM);
        }

        // 취소 실패는 가장 위험: 반드시 기록하고 사용자에게 결제 취소 실패로 안내
        paymentTxService.commitRollbackCancelFailed(confirmResponse, reservationId, memberId, failureMessage);
        throw new BusinessException(ErrorCode.PAYMENT_CANCELED_FAILED_AFTER_CONFIRM);
    }
}
