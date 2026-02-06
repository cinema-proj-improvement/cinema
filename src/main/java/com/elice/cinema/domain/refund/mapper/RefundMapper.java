package com.elice.cinema.domain.refund.mapper;

import com.elice.cinema.domain.refund.dto.response.AdminRefundListResponse;
import com.elice.cinema.domain.refund.entity.Refund;
import com.elice.cinema.domain.reservation.entity.Reservation;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RefundMapper {

    default AdminRefundListResponse toAdminRefundListResponse(Refund refund, Reservation reservation) {
        return new AdminRefundListResponse(
                refund.getId(),
                refund.getRefundAmount(),
                refund.getRefundedAt(),
                reservation.getMemberName(),
                reservation.getReservationCode(),
                reservation.getMovieTitle()
        );
    }
}
