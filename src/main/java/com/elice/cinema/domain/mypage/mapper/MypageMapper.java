package com.elice.cinema.domain.mypage.mapper;

import com.elice.cinema.domain.member.entity.Member;
import com.elice.cinema.domain.mypage.dto.MypageHomeResponse;
import com.elice.cinema.domain.reservation.dto.response.MypageReservationResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MypageMapper {
    default MypageHomeResponse toMypageHomeResponse(Member member,
                                                    List<MypageReservationResponse> reservations) {
        return new MypageHomeResponse(
                member.getNickname(),
                reservations
        );
    }
}
