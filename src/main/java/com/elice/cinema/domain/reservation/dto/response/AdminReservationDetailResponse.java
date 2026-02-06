package com.elice.cinema.domain.reservation.dto.response;

import com.elice.cinema.domain.payment.entity.PaymentStatus;
import com.elice.cinema.domain.reservation.entity.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class AdminReservationDetailResponse {

    // ===== 예매 기본 =====
    private Long reservationId;
    private String reservationCode;
    private ReservationStatus reservationStatus;
    private LocalDateTime reservedAt;

    // ===== 예매자 정보 =====
    private String memberName;
    private String memberLoginId;

    // ===== 상영 정보 =====
    private String movieTitle;
    private String screenName;
    private LocalDate screeningDate;
    private LocalTime screeningStartTime;

    // ===== 좌석 정보 =====
    private List<String> seatCodes;
    private int seatCount;

    // ===== 결제 정보 =====
    private Integer totalPrice;
    private PaymentStatus paymentStatus;

    // ===== 화면 제어용 =====
    private boolean cancelable;

    public AdminReservationDetailResponse withCancelable(boolean cancelable) {
        return new AdminReservationDetailResponse(
                reservationId,
                reservationCode,
                reservationStatus,
                reservedAt,
                memberName,
                memberLoginId,
                movieTitle,
                screenName,
                screeningDate,
                screeningStartTime,
                seatCodes,
                seatCount,
                totalPrice,
                paymentStatus,
                cancelable
        );
    }

}
