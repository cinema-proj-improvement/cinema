package com.elice.cinema.domain.screening.entity;

import com.elice.cinema.domain.common.ScreeningType;
import com.elice.cinema.domain.movie.entity.Movie;
import com.elice.cinema.domain.screen.entity.Screen;
import com.elice.cinema.global.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "screenings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_screening_screen_start", columnNames = {"screen_id", "start_at"})
        },
        indexes = {
                @Index(name = "ix_screening_movie", columnList = "movie_id,start_at"),
                @Index(name = "ix_screening_screen_time", columnList = "screen_id,start_at,end_at_with_cleaning")
        }
)
public class Screening extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    @Enumerated(EnumType.STRING)
    @Column(name = "screening_type", nullable = false, length = 30)
    private ScreeningType screeningType;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)                 // 실제 상영 종료 시간 (표시용)
    private LocalDateTime endAt;

    @Column(name = "end_at_with_cleaning", nullable = false)   // 실제 상영 종료 시간 + 청소 시간 (정책용)
    private LocalDateTime endAtWithCleaning;

    @Enumerated(EnumType.STRING)
    @Column(name = "screening_status", nullable = false, length = 20)
    private ScreeningStatus screeningStatus;

    private Screening(Movie movie,
                      Screen screen,
                      ScreeningType screeningType,
                      LocalDateTime startAt,
                      LocalDateTime endAt,
                      LocalDateTime endAtWithCleaning,
                      ScreeningStatus screeningStatus) {
        this.movie = movie;
        this.screen = screen;
        this.screeningType = screeningType;
        this.startAt = startAt;
        this.endAt = endAt;
        this.endAtWithCleaning = endAtWithCleaning;
        this.screeningStatus = screeningStatus;
    }

    public static Screening of(Movie movie,
                               Screen screen,
                               ScreeningType screeningType,
                               LocalDateTime startAt,
                               LocalDateTime endAt,
                               LocalDateTime endAtWithCleaning,
                               ScreeningStatus screeningStatus) {
        return new Screening(
                movie,
                screen,
                screeningType,
                startAt,
                endAt,
                endAtWithCleaning,
                screeningStatus);
    }

    public void assignScreen(Screen screen) {
        this.screen = screen;
    }

    public void updateScreeningStatus(ScreeningStatus screeningStatus) {
        this.screeningStatus = screeningStatus;
    }

}
