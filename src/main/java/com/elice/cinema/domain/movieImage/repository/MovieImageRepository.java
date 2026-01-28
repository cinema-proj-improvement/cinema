package com.elice.cinema.domain.movieImage.repository;

import com.elice.cinema.domain.movieImage.entity.MovieImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MovieImageRepository extends JpaRepository<MovieImage, Long> {

    // 대표 포스터 (displayOrder = 0)
    @Query("""
        select mi.imageUrl
        from MovieImage mi
        where mi.movie.id = :movieId
          and mi.displayOrder = 0
    """)
    Optional<String> findThumbnailUrlByMovieId(@Param("movieId") Long movieId);

    // 엑스트라 이미지 (displayOrder > 0)
    @Query("""
        select mi.imageUrl
        from MovieImage mi
        where mi.movie.id = :movieId
          and mi.displayOrder > 0
        order by mi.displayOrder asc
    """)
    List<String> findExtraImagesByMovieId(@Param("movieId") Long movieId);

}
