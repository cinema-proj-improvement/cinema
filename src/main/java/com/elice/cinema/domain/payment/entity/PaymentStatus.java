package com.elice.cinema.domain.payment.entity;

public enum PaymentStatus {
    PAID,               // 결제 완료
    CANCELED,           // 결제 취소
    CANCEL_FAILED;       // 승인 완료 후 취소 시도했으나 실패

    public boolean canChangeTo(PaymentStatus target) {
        // no-op 허용
        if (this == target) return true;

        return switch (this) {
            case PAID -> target == CANCELED || target == CANCEL_FAILED; // PG 또는 관리자에 의한 취소 가능여부를 비즈니스 정책에 따라 허용/금지
            case CANCELED -> false; // 취소 후 변경 불가
            case CANCEL_FAILED -> target == CANCELED;
            default -> false;
        };
    }
}

