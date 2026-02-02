package com.elice.cinema.domain.reservation.mapper;

import com.elice.cinema.domain.reservation.dto.response.AdminReservationDetailResponse;
import com.elice.cinema.domain.reservation.dto.response.AdminReservationListResponse;
import com.elice.cinema.domain.reservation.dto.response.ReservationCheckoutResponse;
import com.elice.cinema.domain.reservation.entity.Reservation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReservationMapper {
    public ReservationCheckoutResponse toReservationCheckoutResponse(Reservation reservation,
                                                                     String movieThumbnail,
                                                                     List<String> seatCodes);

    @Mapping(target = "seatSummary", source = "seatSummary")
    @Mapping(target = "paymentStatus", source = "paymentStatus")
    AdminReservationListResponse toAdminListResponse(
            Reservation reservation,
            String seatSummary,
            String paymentStatus
    );

    @Mapping(target = "seatCodes", source = "seatCodes")
    @Mapping(target = "paymentStatus", source = "paymentStatus")
    AdminReservationDetailResponse toAdminDetailResponse(
            Reservation reservation,
            List<String> seatCodes,
            String paymentStatus
    );
}
