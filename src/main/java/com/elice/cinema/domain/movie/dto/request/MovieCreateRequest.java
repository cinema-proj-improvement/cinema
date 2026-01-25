package com.elice.cinema.domain.movie.dto.request;

import com.elice.cinema.domain.movie.entity.AgeRating;
import jakarta.validation.constraints.*;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class MovieCreateRequest {
    @NotBlank(message = "영화 제목은 필수입니다.")
    @Size(max = 255, message = "영화 제목은 255자 이내여야 합니다.")
    private String title;

    @Min(value = 1, message = "러닝타임은 1분 이상이어야 합니다.")
    private int runningTimeMinutes;

    @NotNull(message = "개봉일은 필수입니다.")
    @FutureOrPresent(message = "개봉일은 오늘 이후여야 합니다.")
    private LocalDate releaseDate;

    @NotNull(message = "상영 종료일은 필수입니다.")
    @Future(message = "상영 종료일은 미래 날짜여야 합니다.")
    private LocalDate endDate;

    @NotNull(message = "관람 등급은 필수입니다.")
    private AgeRating ageRating;

    @NotBlank(message = "시놉시스는 필수입니다.")
    private String synopsis;

    @Size(max = 500, message = "썸네일 URL은 500자 이내여야 합니다.")
    private String thumbnailImageUrl;
}
