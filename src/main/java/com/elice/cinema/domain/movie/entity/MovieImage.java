package com.elice.cinema.domain.movie.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(  // FIXME: Entity 코드에서 하지 말고 DB에서 설정하는 게 낫지 않나 싶음...
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
public class MovieImage {  // TODO: Movie에 양방향 연관관계 맺어주지 않을 경우 DB level에서 movie_id FK에 ON DELETE CASCADE 걸어줘야 함
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_thumbnail",  nullable = false)
    private boolean isThumbnail;
}
