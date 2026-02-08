package com.elice.cinema.domain.reservation.mapper;

import com.elice.cinema.domain.reservation.dto.response.*;
import com.elice.cinema.domain.reservation.entity.Reservation;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReservationMapper {
    public ReservationCheckoutResponse toReservationCheckoutResponse(Reservation reservation,
                                                                     String movieThumbnail,
                                                                     List<String> seatCodes);
    public TossPaymentReservationResponse toPaymentReservationResponse(Reservation reservation,
                                                                       String orderId,
                                                                       String tossClientKey);
    public MypageHomeReservationResponse toMypageReservationResponse(Reservation reservation, List<String> seatCodes);
    public MypageDetailReservationResponse toMypageDetailReservationResponse(Reservation reservation,
                                                                             List<String> seatCodes);
    public ReservationIdResponse toReservationIdResponse(Reservation reservation);
}
