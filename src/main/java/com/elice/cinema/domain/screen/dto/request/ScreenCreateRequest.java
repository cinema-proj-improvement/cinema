package com.elice.cinema.domain.screen.dto.request;

import com.elice.cinema.domain.common.ScreeningType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ScreenCreateRequest {

    @NotBlank(message = "상영관 이름은 필수입니다.")
    @Size(max = 50, message = "상영관 이름은 최대 50자까지 가능합니다.")
    private String name;

    @NotNull(message = "상영 타입은 필수입니다.")
    private ScreeningType screeningType;

    @NotNull(message = "총 좌석 수는 필수입니다.")
    @Min(value = 1, message = "총 좌석 수는 1 이상이어야 합니다.")
    private Integer totalSeats;

    @NotNull(message = "운영 여부는 필수입니다.")
    private Boolean operating;

    @NotEmpty(message = "좌석 정보는 최소 1개 이상 필요합니다.")
    @Valid
    private List<SeatCreateRequest> seats;
}
