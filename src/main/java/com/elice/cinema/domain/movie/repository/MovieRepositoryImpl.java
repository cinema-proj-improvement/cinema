package com.elice.cinema.domain.movie.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MovieRepositoryImpl implements MovieRepositoryCustom {
    private final JPAQueryFactory queryFactory;
}
