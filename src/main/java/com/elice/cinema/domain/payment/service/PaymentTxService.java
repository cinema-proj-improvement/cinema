package com.elice.cinema.domain.payment.service;

import com.elice.cinema.domain.member.entity.Member;
import com.elice.cinema.domain.member.repository.MemberRepository;
import com.elice.cinema.domain.payment.dto.response.TossConfirmResponse;
import com.elice.cinema.domain.payment.entity.Payment;
import com.elice.cinema.domain.payment.entity.PaymentStatus;
import com.elice.cinema.domain.payment.mapper.PaymentMapper;
import com.elice.cinema.domain.payment.repository.PaymentRepository;
import com.elice.cinema.domain.policy.dto.response.RefundCalculationResult;
import com.elice.cinema.domain.refund.service.RefundService;
import com.elice.cinema.domain.reservation.entity.Reservation;
import com.elice.cinema.domain.reservation.entity.ReservationStatus;
import com.elice.cinema.domain.reservation.entity.ReservedSeat;
import com.elice.cinema.domain.reservation.repository.ReservationRepository;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentTxService {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final PaymentMapper paymentMapper;
    private final RefundService refundService;

    @Transactional
    public void commitPaymentSuccess(TossConfirmResponse res, Long reservationId, Long memberId) {
        if (paymentRepository.existsByPaymentKey(res.getPaymentKey())) {
            log.warn("[Payment] 중복 결제 승인 요청 무시: paymentKey={}, reservationId={}", res.getPaymentKey(), reservationId);
            return; // 멱등 처리
        }

        Reservation reservation = reservationRepository.findWithReservedSeatsById(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

        if (reservation.getStatus() != ReservationStatus.HOLD) {
            log.warn("[Payment] HOLD 상태가 아닌 예약에 대한 결제 승인 시도: reservationId={}, currentStatus={}", reservationId, reservation.getStatus());
            throw new BusinessException(ErrorCode.RESERVATION_INVALID_STATUS);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        List<ReservedSeat> reservedSeats = reservation.getReservedSeats();

        Long totalPrice = reservation.getTotalPrice().longValue();

        // 승인된 금액도 같은지 확인
        if (!totalPrice.equals(res.getTotalAmount())) {
            log.error("[Payment] 결제 금액 불일치: reservationId={}, expected={}, actual={}", reservationId, totalPrice, res.getTotalAmount());
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        reservation.confirm();
        reservedSeats.forEach(ReservedSeat::confirm);

        Payment payment = paymentMapper.toEntity(res, reservation, member);
        paymentRepository.save(payment);

        log.info("[Payment] 결제 승인 커밋 완료: reservationId={}, memberId={}, amount={}", reservationId, memberId, res.getTotalAmount());
    }

    @Transactional
    public void commitRollbackCanceled(TossConfirmResponse res,
                                       Long reservationId,
                                       Long memberId,
                                       String failureMessage) {
        Payment payment = getOrCreatePayment(res, reservationId, memberId);

        Reservation reservation = getReservationById(reservationId);

        if (payment.getStatus() == PaymentStatus.CANCELED) {
            log.warn("[Payment] 이미 취소된 결제 롤백 요청 무시: reservationId={}", reservationId);
            return;
        }

        log.warn("[Payment] 결제 승인 후 처리 실패로 롤백 시작: reservationId={}, reason={}", reservationId, failureMessage);

        payment.markCanceled(failureMessage);

        reservation.fail();

        paymentRepository.save(payment); //FIXME: res에서 널 값이 들어오면 터짐, res 검증 로직이 필요할 듯

        log.warn("[Payment] 결제 롤백 완료 (취소 처리): reservationId={}", reservationId);
    }

    @Transactional
    public void commitRollbackCancelFailed(TossConfirmResponse res,
                                           Long reservationId,
                                           Long memberId,
                                           String failureMessage) {
        Payment payment = getOrCreatePayment(res, reservationId, memberId);

        if (payment.getStatus() == PaymentStatus.CANCEL_FAILED) {
            log.warn("[Payment] 이미 취소 실패 상태인 결제 재처리 요청 무시: reservationId={}", reservationId);
            return;
        }

        // 결제는 승인됐으나 취소 불가 — 수동 처리 필요
        log.error("[Payment] 결제 롤백 취소 실패 (CANCEL_FAILED): reservationId={}, paymentKey={}, reason={}",
                reservationId, res.getPaymentKey(), failureMessage);

        payment.markCancelFailed(failureMessage);
        paymentRepository.save(payment);
    }

    @Transactional
    public void commitCancelSuccess(
            Long paymentId,
            RefundCalculationResult result) {
        Payment payment = paymentRepository.findByIdWithReservation(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() == PaymentStatus.CANCELED) {
            log.warn("[Payment] 이미 취소된 결제 취소 요청 무시: paymentId={}", paymentId);
            return;
        }

        Reservation reservation = payment.getReservation();

        reservation.cancel();

        payment.markCanceled(result.getReason());

        refundService.createRefund(payment, result.getCancelAmount());

        log.info("[Payment] 결제 취소 커밋 완료: paymentId={}, cancelAmount={}, reason={}",
                paymentId, result.getCancelAmount(), result.getReason());
    }

    // TODO: 결제 실패 메시지안에 결제 취소 이유를 넣어야하나? 아니면 결제 취소 실패 이유를 넣어야 하나?
    @Transactional
    public void commitCancelFailed(
            Long paymentId,
            RefundCalculationResult result) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() == PaymentStatus.CANCEL_FAILED) {
            log.warn("[Payment] 이미 취소 실패 상태인 결제 재처리 요청 무시: paymentId={}", paymentId);
            return;
        }

        log.warn("[Payment] 결제 취소 실패 커밋: paymentId={}, reason={}", paymentId, result.getReason());

        payment.markCancelFailed(result.getReason());
    }

    private Payment getOrCreatePayment(TossConfirmResponse res, Long reservationId, Long memberId) {
        return paymentRepository.findByPaymentKey(res.getPaymentKey())
                .orElseGet(() -> {
                    Reservation reservation = reservationRepository.findById(reservationId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
                    Member member = memberRepository.findById(memberId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
                    return paymentMapper.toEntity(res, reservation, member);
                });
    }

    private Reservation getReservationById(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
    }
}
