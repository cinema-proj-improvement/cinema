package com.elice.cinema.domain.review.entity;

import com.elice.cinema.domain.member.entity.Member;
import com.elice.cinema.domain.movie.entity.Movie;
import com.elice.cinema.domain.reservation.entity.Reservation;
import com.elice.cinema.global.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "reviews",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_review_reservation", columnNames = "reservation_id")
        },
        indexes = {
                @Index(name = "ix_review_movie_created", columnList = "movie_id,created_at"),
                @Index(name = "ix_review_member_created", columnList = "member_id,created_at")
        }
)
public class Review extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    private Review(Member member,
                   Movie movie,
                   Integer score,
                   String content) {
        this.member = member;
        this.movie = movie;
        this.score = score;
        this.content = content;
    }

    public static Review of(Member member,
                            Movie movie,
                            Integer score,
                            String content) {
        return new Review(member, movie, score, content);
    }
}
