package com.elice.cinema.domain.reservation.dto.response.seatselection;

import lombok.*;

import java.util.List;

@Getter
@AllArgsConstructor(access =  AccessLevel.PRIVATE)
public class SeatSelectionResponse {
    private ScreeningInfo screeningInfo;
    private ScreenInfo screenInfo;
    private List<SeatInfo> seatInfos;
    private int maxReservationCount;
    private int defaultPrice;

    public static SeatSelectionResponse of(
            ScreeningInfo screeningInfo,
            ScreenInfo screenInfo,
            List<SeatInfo> seatInfos,
            int maxReservationCount,
            int defaultPrice
    ) {
        SeatSelectionResponse res = new SeatSelectionResponse(
                screeningInfo,
                screenInfo,
                seatInfos,
                maxReservationCount,
                defaultPrice);

        return res;
    }
}