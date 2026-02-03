package com.elice.cinema.domain.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentMemberResponse {
    private Long id;
    private String email;
    private String name;
}
