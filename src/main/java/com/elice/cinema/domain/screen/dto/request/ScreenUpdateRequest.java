package com.elice.cinema.domain.screen.dto.request;

import com.elice.cinema.domain.common.ScreeningType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScreenUpdateRequest {
    @NotBlank(message = "상영관 이름은 필수입니다.")
    @Size(max = 50, message = "상영관 이름은 최대 50자까지 가능합니다.")
    private String name;

    @NotNull(message = "상영 타입은 필수입니다.")
    private ScreeningType screeningType;

    @NotNull(message = "운영 여부는 필수입니다.")
    private Boolean operating;
}
