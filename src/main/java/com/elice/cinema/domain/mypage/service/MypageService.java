package com.elice.cinema.domain.mypage.service;

import com.elice.cinema.domain.member.entity.Member;
import com.elice.cinema.domain.member.repository.MemberRepository;
import com.elice.cinema.domain.mypage.dto.MypageHomeResponse;
import com.elice.cinema.domain.mypage.mapper.MypageMapper;
import com.elice.cinema.domain.reservation.dto.response.MypageReservationResponse;
import com.elice.cinema.domain.reservation.entity.Reservation;
import com.elice.cinema.domain.reservation.entity.ReservationStatus;
import com.elice.cinema.domain.reservation.entity.ReservedSeat;
import com.elice.cinema.domain.reservation.mapper.ReservationMapper;
import com.elice.cinema.domain.reservation.repository.ReservationRepository;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MypageService {
    private final MemberRepository memberRepository;
    private final ReservationRepository reservationRepository;
    private final MypageMapper mypageMapper;
    private final ReservationMapper reservationMapper;

    public MypageHomeResponse getMypageHome(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 마이페이지 홈에 필요한 예약 목록 + 상세(스크리닝/좌석) 조회
        List<Reservation> reservations = reservationRepository.findTop3ByMemberIdOrderByReservedAtDesc(memberId);

        List<MypageReservationResponse> reservationResponses = reservations.stream()
                .map(r -> reservationMapper.toMypageReservationResponse(r, extractSeatCodes(r)))
                .toList();

        return mypageMapper.toMypageHomeResponse(member, reservationResponses);
    }

    private List<String> extractSeatCodes(Reservation reservation) {
        return reservation.getReservedSeats().stream()
                .filter(rs -> rs.getStatus() != ReservationStatus.CANCELED)
                .map(ReservedSeat::getSeatCode)
                .toList();
    }
}
