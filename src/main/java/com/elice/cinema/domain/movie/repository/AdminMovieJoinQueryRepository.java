package com.elice.cinema.domain.movie.repository;

import com.elice.cinema.domain.movie.dto.request.AdminMovieSortType;
import com.elice.cinema.domain.movie.dto.response.AdminMovieJoinRowResponse;

import java.util.List;

public interface AdminMovieJoinQueryRepository {

    List<AdminMovieJoinRowResponse> findAdminMovieJoinRows(List<Long> movieIds, AdminMovieSortType sortType);
}
