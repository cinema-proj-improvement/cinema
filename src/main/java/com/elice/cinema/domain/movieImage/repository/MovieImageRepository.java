package com.elice.cinema.domain.movieImage.repository;

import com.elice.cinema.domain.movieImage.entity.MovieImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MovieImageRepository extends JpaRepository<MovieImage, Long> {
    // 썸네일(0)
    Optional<MovieImage> findByMovieIdAndDisplayOrder(Long movieId, Integer displayOrder);

    // extra(>=1)
    List<MovieImage> findByMovieIdAndDisplayOrderGreaterThanEqualOrderByDisplayOrderAsc(Long movieId, Integer displayOrder);

    // 전체(0 포함)
    List<MovieImage> findByMovieIdOrderByDisplayOrderAsc(Long movieId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from MovieImage mi where mi.movie.id = :movieId")
    int deleteAllByMovieId(@Param("movieId") Long movieId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from MovieImage mi where mi.movie.id = :movieId and mi.displayOrder = 0")
    int deleteThumbnailByMovieId(@Param("movieId") Long movieId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from MovieImage mi where mi.movie.id = :movieId and mi.displayOrder >= 1")
    int deleteExtrasByMovieId(@Param("movieId") Long movieId);
}
