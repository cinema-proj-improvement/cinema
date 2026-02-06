package com.elice.cinema.domain.payment.dto.response;

import lombok.Getter;

import java.util.List;

@Getter
public class TossCancelResponse {
    private String paymentKey;
    private String orderId;
    private String status;
    private List<CancelDetail> cancels;

    @Getter
    public static class CancelDetail {
        private Long cancelAmount;
        private String cancelReason;
        private String canceledAt;
    }

    /** 이번 요청으로 취소된 금액 */
    public long getLatestCancelAmount() {
        if (cancels == null || cancels.isEmpty()) {
            return 0L;
        }
        return cancels.get(cancels.size() - 1).getCancelAmount();
    }
}
