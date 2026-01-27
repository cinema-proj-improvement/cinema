package com.elice.cinema.domain.movieImage.repository;

import com.elice.cinema.domain.movieImage.entity.MovieImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieImageRepository extends JpaRepository<MovieImage, Long> {
}
