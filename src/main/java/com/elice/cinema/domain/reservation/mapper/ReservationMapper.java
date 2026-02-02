package com.elice.cinema.domain.reservation.mapper;

import com.elice.cinema.domain.reservation.entity.Reservation;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReservationMapper {
    MemberReservationPageSummaryInfoResponse toMemberReservationPageSummaryInfoResponse(Reservation reservation);
}