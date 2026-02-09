package com.elice.cinema.domain.movie.dto.request;

import com.elice.cinema.domain.movie.entity.AgeRating;
import com.elice.cinema.domain.movie.entity.Genre;
import com.elice.cinema.domain.common.ScreeningType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
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

    @NotEmpty(message = "장르는 최소 1개 이상 선택해야 합니다.")
    private Set<Genre> genres = new HashSet<>();

    @NotEmpty(message = "상영 타입은 최소 1개 이상 선택해야 합니다.")
    private Set<ScreeningType> screeningTypes = new HashSet<>();

    @NotBlank(message = "시놉시스는 필수입니다.")
    private String synopsis;

    @NotNull(message = "대표 포스터는 필수입니다.")
    private MultipartFile thumbnailImage;

    @Size(max = 10, message = "추가 이미지는 최대 10개까지 가능합니다.")
    private List<MultipartFile> extraImages = new ArrayList<>();
}
