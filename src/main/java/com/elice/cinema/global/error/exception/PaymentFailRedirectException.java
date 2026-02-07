package com.elice.cinema.global.error.exception;

import com.elice.cinema.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class PaymentFailRedirectException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String orderId;

    public PaymentFailRedirectException(ErrorCode errorCode, String orderId) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.orderId = orderId;
    }
}
