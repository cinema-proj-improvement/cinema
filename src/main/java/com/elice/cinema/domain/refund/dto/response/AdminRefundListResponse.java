package com.elice.cinema.domain.refund.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminRefundListResponse {
    private Long id;
    private Long refundAmount;
    private LocalDateTime refundedAt;

    private String memberName;
    private String reservationCode;
    private String movieTitle;
}
