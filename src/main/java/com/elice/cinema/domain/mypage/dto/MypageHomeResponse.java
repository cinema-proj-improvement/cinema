package com.elice.cinema.domain.mypage.dto;

import com.elice.cinema.domain.reservation.dto.response.MypageHomeReservationResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MypageHomeResponse {
    private String nickname;

    private List<MypageHomeReservationResponse> reservations;
}
