package com.elice.cinema.domain.screen.mapper;

import com.elice.cinema.domain.screen.dto.response.SeatDetailResponse;
import com.elice.cinema.domain.screen.entity.Seat;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SeatMapper {
    public abstract SeatDetailResponse toSeatDetailResponse(Seat seat);
}
