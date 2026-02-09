package com.elice.cinema.domain.movieImage.entity;

import com.elice.cinema.domain.movie.entity.Movie;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "movie_images",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UK_movie_image_movie_display_order",
                        columnNames = {"movie_id", "display_order"}
                )
        },
        indexes = {
                @Index(
                        name = "IX_movie_images_movie_id",
                        columnList = "movie_id"
                ),
                @Index(
                        name = "IX_movie_images_movie_id_display_order",
                        columnList = "movie_id, display_order"
                )
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class MovieImage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;  // 0: 썸네일, 1...n: 추가 이미지

    private MovieImage(Movie movie, String imageUrl, int displayOrder) {
        this.movie = movie;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
    }

    public static MovieImage thumbnail(Movie movie, String imageUrl) {
        return new MovieImage(movie, imageUrl, 0);
    }

    public static MovieImage extra(Movie movie, String imageUrl, int displayOrder) {
        if (displayOrder <= 0) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return new MovieImage(movie, imageUrl, displayOrder);
    }

    public boolean isThumbnail() {
        return this.displayOrder != null && this.displayOrder == 0;
    }

    public void changeDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
