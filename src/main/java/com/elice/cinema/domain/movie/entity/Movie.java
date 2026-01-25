package com.elice.cinema.domain.movie.entity;

import com.elice.cinema.domain.movie.entity.AgeRating;
import com.elice.cinema.domain.movie.entity.MovieStatus;
import com.elice.cinema.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDate;

@Entity
@Getter
@Table(name = "movie")
public class Movie extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
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
    @Column(nullable = false)
    private String synopsis;

    @Column(name = "thumbnail_image_url", nullable = false, length = 500)
    private String thumbnailImageUrl;

    @Column(name = "avg_score")
    private Double avgScore;

    @Column(name = "advance_reservation_rate")
    private Double advanceReservationRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovieStatus status;

    protected Movie() {
    }
}
