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
import com.elice.cinema.domain.reservation.entity.ReservedSeat;
import com.elice.cinema.domain.reservation.repository.ReservationRepository;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
            return; // 멱등 처리
        }

        //TODO: 여기서도 HOLD 상태인지 검증해줘야 하나?
        Reservation reservation = reservationRepository.findWithReservedSeatsById(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        List<ReservedSeat> reservedSeats = reservation.getReservedSeats();

        Long totalPrice = reservation.getTotalPrice().longValue();

        // 승인된 금액도 같은지 확인
        if (!totalPrice.equals(res.getTotalAmount())) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        reservation.confirm();
        reservedSeats.forEach(ReservedSeat::confirm);

        Payment payment = paymentMapper.toEntity(res, reservation, member);
        paymentRepository.save(payment);
    }

    @Transactional
    public void commitRollbackCanceled(TossConfirmResponse res,
                                       Long reservationId,
                                       Long memberId,
                                       String failureMessage) {
        Payment payment = getOrCreatePayment(res, reservationId, memberId);

        Reservation reservation = getReservationById(reservationId);

        if (payment.getStatus() == PaymentStatus.CANCELED) return;


        payment.markCanceled(failureMessage);

        reservation.fail();

        paymentRepository.save(payment); //FIXME: res에서 널 값이 들어오면 터짐, res 검증 로직이 필요할 듯
    }

    @Transactional
    public void commitRollbackCancelFailed(TossConfirmResponse res,
                                           Long reservationId,
                                           Long memberId,
                                           String failureMessage) {
        Payment payment = getOrCreatePayment(res, reservationId, memberId);

        if (payment.getStatus() == PaymentStatus.CANCEL_FAILED) return;


        payment.markCancelFailed(failureMessage);
        paymentRepository.save(payment);
    }

    @Transactional
    public void commitCancelSuccess(
            Long paymentId,
            RefundCalculationResult result) {
        Payment payment = paymentRepository.findByIdWithReservation(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() == PaymentStatus.CANCELED) return;

        Reservation reservation = payment.getReservation();

        reservation.cancel();

        payment.markCanceled(result.getReason());

        refundService.createRefund(payment, result.getCancelAmount());
    }

    // TODO: 결제 실패 메시지안에 결제 취소 이유를 넣어야하나? 아니면 결제 취소 실패 이유를 넣어야 하나?
    @Transactional
    public void commitCancelFailed(
            Long paymentId,
            RefundCalculationResult result) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() == PaymentStatus.CANCEL_FAILED) return;

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
