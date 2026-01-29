package com.elice.cinema.domain.movie.repository;

import com.elice.cinema.domain.movie.dto.internal.AdminMovieJoinRow;

import java.util.List;

public interface AdminMovieJoinQueryRepository {

    List<AdminMovieJoinRow> findAdminMovieJoinRows(List<Long> movieIds);
}
