package com.elice.cinema.domain.screen.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeatCreateRequest {

    @NotBlank(message = "좌석 코드는 필수입니다.")
    @Size(max = 10, message = "좌석 코드는 최대 10자까지 가능합니다.")
    @Pattern(
            regexp = "^[A-Z]+[0-9]+$",
            message = "좌석 코드는 대문자 영문 + 숫자 형식이어야 합니다. (예: A1, B12)"
    )
    private String seatCode;

    @NotNull(message = "좌석 활성 여부는 필수입니다.")
    private Boolean active;

    @NotNull(message = "좌석 행 번호는 필수입니다.")
    @Min(value = 1, message = "좌석 행 번호는 1 이상이어야 합니다.")
    private Integer rowNo;

    @NotNull(message = "좌석 열 번호는 필수입니다.")
    @Min(value = 1, message = "좌석 열 번호는 1 이상이어야 합니다.")
    private Integer colNo;
}
