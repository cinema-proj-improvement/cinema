package com.elice.cinema.domain.payment.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TossCancelRequest {
    private String cancelReason;
}
