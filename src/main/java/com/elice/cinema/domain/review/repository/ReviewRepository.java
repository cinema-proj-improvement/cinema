package com.elice.cinema.domain.review.repository;

import com.elice.cinema.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
}
