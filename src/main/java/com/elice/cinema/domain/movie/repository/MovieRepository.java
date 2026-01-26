package com.elice.cinema.domain.movie.repository;

import com.elice.cinema.domain.movie.entity.Movie;
import com.elice.cinema.domain.movie.entity.MovieStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    // 상태별 조회
    Page<Movie> findByStatus(MovieStatus status, Pageable pageable);

    // 제목 검색 (부분 일치)
    Page<Movie> findByTitleContainingIgnoreCase(String title, Pageable pageable);
}
