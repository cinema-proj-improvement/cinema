package com.elice.cinema.domain.screen.mapper;

import com.elice.cinema.domain.reservation.dto.response.seatselection.SeatInfo;
import com.elice.cinema.domain.screen.dto.response.SeatDetailResponse;
import com.elice.cinema.domain.screen.entity.Seat;
import org.mapstruct.Mapper;

import java.util.Set;

@Mapper(componentModel = "spring")
public interface SeatMapper {
    public abstract SeatDetailResponse toSeatDetailResponse(Seat seat);

    // 좌석 선택 페이지에서 필요로 하는 좌석 정보로 변환
    default SeatInfo toSeatInfo(Seat seat, Set<Long> blockedList) {
        SeatInfo res = new SeatInfo();
        res.setSeatId(seat.getId());
        res.setSeatCode(seat.getSeatCode());
        res.setRowNo(seat.getRowNo());
        res.setColNo(seat.getColNo());

        boolean selectable = seat.isActive() && !blockedList.contains(seat.getId());
        res.setSelectable(selectable);

        return res;
    }
}
