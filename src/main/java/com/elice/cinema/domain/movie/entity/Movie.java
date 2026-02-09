package com.elice.cinema.domain.movie.entity;

import com.elice.cinema.domain.common.ScreeningType;
import com.elice.cinema.global.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "movies",
        indexes = {
                // 상태 + 개봉일 (개봉 전 → 상영중 배치)
                @Index(
                        name = "idx_movie_status_release_date",
                        columnList = "status, release_date"
                ),

                // 상태 + 상영 종료일 (상영중 → 종료 배치)
                @Index(
                        name = "idx_movie_status_end_date",
                        columnList = "status, end_date"
                )
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Movie extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "running_time_minutes", nullable = false)
    private Integer runningTimeMinutes;

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

    @ElementCollection(fetch = FetchType.LAZY)
    @Enumerated(EnumType.STRING)
    @Column(name = "genre", nullable = false, length = 30)
    private Set<Genre> genres = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @Enumerated(EnumType.STRING)
    @Column(name = "screening_type", nullable = false, length = 30)
    private Set<ScreeningType> screeningTypes =  new HashSet<>();

    @Column(name = "avg_score", nullable = false)
    private Double avgScore;

    @Column(name = "advance_reservation_rate", nullable = false)
    private Double advanceReservationRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MovieStatus status;

    public static Movie createUpcoming(String title,
                                       int runningTimeMinutes,
                                       LocalDate releaseDate,
                                       LocalDate endDate,
                                       AgeRating ageRating,
                                       String synopsis,
                                       Set<Genre> genres,
                                       Set<ScreeningType> screeningTypes) {  // 썸네일 이미지 주소는 객체 생성 시 포함되지 않습니다.
        return new Movie(title,
                runningTimeMinutes,
                releaseDate,
                endDate,
                ageRating,
                synopsis,
                genres,
                screeningTypes
        );
    }

    public void changeBasicInfo(String title,
                                Integer runningTimeMinutes,
                                AgeRating ageRating,
                                String synopsis) {
        this.title = title;
        this.runningTimeMinutes = runningTimeMinutes;
        this.ageRating = ageRating;
        this.synopsis = synopsis;
    }

    public void changeGenres(Set<Genre> genres) {
        this.genres = new HashSet<>(genres);
    }

    public void changeScreeningTypes(Set<ScreeningType> screeningTypes) {
        this.screeningTypes = new HashSet<>(screeningTypes);
    }

    private Movie(String title,
                  int runningTimeMinutes,
                  LocalDate releaseDate,
                  LocalDate endDate,
                  AgeRating ageRating,
                  String synopsis,
                  Set<Genre> genres,
                  Set<ScreeningType> screeningTypes) {
        this.title = title;
        this.runningTimeMinutes = runningTimeMinutes;
        this.releaseDate = releaseDate;
        this.endDate = endDate;
        this.ageRating = ageRating;
        this.synopsis = synopsis;
        this.genres = new HashSet<>(genres);
        this.screeningTypes = new HashSet<>(screeningTypes);
        this.avgScore = 0.0;
        this.advanceReservationRate = 0.0;
        this.status = MovieStatus.UPCOMING;
    }
}
