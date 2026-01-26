package com.elice.cinema.domain.movie.entity;

import com.elice.cinema.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "movies")  // TODO: index 설정 필요. notion page "구현관련" -> "Movie Table Index" 확인
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter @Builder

public class Movie extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "running_time_minutes", nullable = false)
    private int runningTimeMinutes;

    @Column(name = "release_date", nullable = false)
    private LocalDate releaseDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_rating", nullable = false, length = 20)
    private AgeRating ageRating;

    @Lob
    @Column(name = "synopsis", nullable = false)
    private String synopsis;

    @Column(name = "thumbnail_image_url", nullable = false, length = 500)
    private String thumbnailImageUrl;  // TODO: 데이터 정합성에 유의! (MovieImage와 중복 데이터) -> 썸네일 변경 로직은 한 위치에서만 + 둘 다 갱신

    @Column(name = "avg_score")
    private Double avgScore;

    @Column(name = "advance_reservation_rate")
    private Double advanceReservationRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MovieStatus status;


    public static Movie of(
            String title,
            int runningTimeMinutes,
            LocalDate releaseDate,
            LocalDate endDate,
            AgeRating ageRating,
            String synopsis,
            String thumbnailImageUrl) {
        return Movie.builder()  // TODO: private 생성자로 따로 빼서 builder 대신 해당 생성자 호출하는 게 나을지?
                .title(title)
                .runningTimeMinutes(runningTimeMinutes)
                .releaseDate(releaseDate)
                .endDate(endDate)
                .ageRating(ageRating)
                .synopsis(synopsis)
                .thumbnailImageUrl(thumbnailImageUrl)
                .avgScore(0.0)
                .advanceReservationRate(0.0)
                .status(MovieStatus.UPCOMING)
                .build();
    }

}
