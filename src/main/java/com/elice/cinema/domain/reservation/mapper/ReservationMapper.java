package com.elice.cinema.domain.reservation.mapper;

import com.elice.cinema.domain.reservation.dto.response.ReservationCheckoutResponse;
import com.elice.cinema.domain.reservation.entity.Reservation;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReservationMapper {
    public ReservationCheckoutResponse toReservationCheckoutResponse(Reservation reservation,
                                                                     String movieThumbnail,
                                                                     List<String> seatCodes);


}
