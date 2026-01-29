package com.elice.cinema.domain.screening.dto.request;

import com.elice.cinema.domain.common.ScreeningType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ScreeningCreateRequest {
    @NotNull(message = "영화를 선택해주세요.")
    private Long movieId;

    @NotNull(message = "상영관을 선택해주세요.")
    private Long screenId;

    @NotNull(message = "상영 타입을 선택해주세요.")
    private ScreeningType screeningType;

    @NotNull(message = "상영 시작 시간을 입력해주세요.")
    @Future(message = "상영 시작 시간은 현재 이후여야 합니다.")
    private LocalDateTime startAt;
}
