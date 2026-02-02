package com.elice.cinema.domain.screening.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminScreeningSeatSummaryResponse {

    private long total;       // 사용 가능한 전체 좌석
    private long confirmed;   // 예매 확정
    private long hold;        // 예매 대기
    private long available;   // total - confirmed - hold
}
