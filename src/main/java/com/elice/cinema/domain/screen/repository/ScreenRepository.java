package com.elice.cinema.domain.screen.repository;

import com.elice.cinema.domain.screen.entity.Screen;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScreenRepository extends JpaRepository<Screen, Long> {
    Page<Screen> findByOperating(boolean operating, Pageable pageable);
    boolean existsByName(String name);
}
