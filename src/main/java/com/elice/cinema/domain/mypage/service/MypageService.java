package com.elice.cinema.domain.mypage.service;

import com.elice.cinema.domain.member.entity.Member;
import com.elice.cinema.domain.member.repository.MemberRepository;
import com.elice.cinema.domain.mypage.dto.MypageHomeResponse;
import com.elice.cinema.domain.mypage.mapper.MypageMapper;
import com.elice.cinema.domain.payment.entity.Payment;
import com.elice.cinema.domain.payment.repository.PaymentRepository;
import com.elice.cinema.domain.payment.service.PaymentCancelService;
import com.elice.cinema.domain.reservation.dto.response.MypageDetailReservationResponse;
import com.elice.cinema.domain.reservation.dto.response.MypageHomeReservationResponse;
import com.elice.cinema.domain.reservation.entity.Reservation;
import com.elice.cinema.domain.reservation.entity.ReservedSeat;
import com.elice.cinema.domain.reservation.mapper.ReservationMapper;
import com.elice.cinema.domain.reservation.repository.ReservationRepository;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MypageService {
    private final PaymentCancelService paymentCancelService;
    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;
    private final ReservationRepository reservationRepository;
    private final MypageMapper mypageMapper;
    private final ReservationMapper reservationMapper;

    @Transactional(readOnly = true)
    public MypageHomeResponse getMypageHome(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 마이페이지 홈에 필요한 예약 목록 + 상세(스크리닝/좌석) 조회
        List<Reservation> reservations = reservationRepository.findTop3ByMemberIdOrderByReservedAtDesc(memberId);

        List<MypageHomeReservationResponse> reservationResponses = reservations.stream()
                .map(r -> reservationMapper.toMypageReservationResponse(r, extractSeatCodes(r)))
                .toList();

        return mypageMapper.toMypageHomeResponse(member, reservationResponses);
    }

    @Transactional(readOnly = true)
    public Slice<MypageDetailReservationResponse> getMyReservations(Long memberId, LocalDate from, LocalDate to, Pageable pageable) {
        LocalDateTime start = (from != null)
                ? from.atStartOfDay()
                : LocalDate.now().minusMonths(3).atStartOfDay();        // TODO: 3개월 기본 값도 환경변수에 넣어야 할지

        LocalDateTime end = (to != null)
                ? to.plusDays(1).atStartOfDay()
                : LocalDate.now().plusDays(1).atStartOfDay();

        Slice<Reservation> slice =
                reservationRepository.findMyReservationsByPeriod(
                        memberId, start, end, pageable
                );

        return slice.map(r ->
                reservationMapper.toMypageDetailReservationResponse(r, extractSeatCodes(r))
        );
    }

    public void cancelReservation(Long reservationId) {
        Payment payment = paymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        paymentCancelService.cancel(payment.getId());
    }

    private List<String> extractSeatCodes(Reservation reservation) {
        return reservation.getReservedSeats().stream()
                .map(ReservedSeat::getSeatCode)
                .toList();
    }
}
