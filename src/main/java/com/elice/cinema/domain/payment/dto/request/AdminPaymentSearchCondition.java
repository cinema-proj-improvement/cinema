package com.elice.cinema.domain.payment.dto.request;

import com.elice.cinema.domain.payment.entity.PaymentStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class AdminPaymentSearchCondition {

    // 검색
    private String keyword;

    // 상태 필터 (null = 전체)
    private PaymentStatus status;

    // 승인일 기준 날짜 검색
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    // 정렬
    private String sort;


    public void applyDefaultDateIfEmpty() {
        if (this.fromDate == null && this.toDate == null) {
            this.toDate = LocalDate.now();
            this.fromDate = this.toDate.minusDays(7);
        }
    }
}
