package com.elice.cinema.domain.screen.entity;

import com.elice.cinema.domain.common.ScreeningType;
import com.elice.cinema.domain.movie.entity.Movie;
import com.elice.cinema.domain.screening.entity.Screening;
import com.elice.cinema.domain.screening.entity.ScreeningStatus;
import com.elice.cinema.global.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "screens",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_screen_name", columnNames = "name")
        },
        indexes = {
                @Index(name = "ix_screening_type", columnList = "screening_type")
        }
)
public class Screen extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "screening_type", nullable = false, length = 30)
    private ScreeningType screeningType;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    @Column(name = "is_operating", nullable = false)
    private boolean operating;

    @OneToMany(mappedBy = "screen", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Seat> seats = new ArrayList<>();

    @OneToMany(mappedBy = "screen", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Screening> screenings = new ArrayList<>();

    private Screen(String name,
                   ScreeningType screeningType,
                   Integer totalSeats,
                   boolean operating) {
        this.name = name;
        this.screeningType = screeningType;
        this.totalSeats = totalSeats;
        this.operating = operating;
    }

    public static Screen of(String name,
                            ScreeningType screeningType,
                            Integer totalSeats,
                            boolean operating) {
        return new Screen(name, screeningType, totalSeats, operating);
    }

    public void addSeat(String seatCode, boolean active, Integer rowNo, Integer colNo) {
        Seat seat = Seat.of(this, seatCode, active, rowNo, colNo); // screen 없이 생성
        seats.add(seat);
    }

    public void addScreening(Movie movie,
                             ScreeningType screeningType,
                             LocalDateTime startAt,
                             LocalDateTime endAt,
                             LocalDateTime endAtWithCleaning,
                             ScreeningStatus screeningStatus) {
        Screening screening = Screening.of(movie, this, screeningType, startAt, endAt, endAtWithCleaning, screeningStatus); // screen 없이 생성
        screenings.add(screening);
    }

    public void updateAll(String name,
                          ScreeningType screeningType,
                          Boolean operating) {

        this.name = name;
        this.screeningType = screeningType;
        this.operating = operating;
    }
}

