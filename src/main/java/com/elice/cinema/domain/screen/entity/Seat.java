package com.elice.cinema.domain.screen.entity;

import com.elice.cinema.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "seats",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_seat_screen_code", columnNames = {"screen_id", "seat_code"}),
                @UniqueConstraint(name = "uk_seat_screen_row_col", columnNames = {"screen_id", "row_no", "col_no"})
        },
        indexes = {
                @Index(name = "ix_seat_screen", columnList = "screen_id")
        }
)
public class Seat extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    @Column(name = "seat_code", nullable = false, length = 10)
    private String seatCode;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "row_no", nullable = false)
    private Integer rowNo;

    @Column(name = "col_no", nullable = false)
    private Integer colNo;

    private Seat(String seatCode, boolean active, Integer rowNo, Integer colNo) {
        this.seatCode = seatCode;
        this.active = active;
        this.rowNo = rowNo;
        this.colNo = colNo;
    }

    public static Seat of(String seatCode, boolean active, Integer rowNo, Integer colNo) {
        return new Seat(seatCode, active, rowNo, colNo);
    }

    protected void setScreen(Screen screen) {
        this.screen = screen;
    }
}

