package com.elice.cinema.global.home.service;

import com.elice.cinema.domain.movie.repository.MovieRepository;
import com.elice.cinema.global.home.dto.response.HomeMovieResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final MovieRepository movieRepository;

    @Transactional(readOnly = true)
    public List<HomeMovieResponse> getTop4Movies() {

        return movieRepository
                .findTopHomeMovies(PageRequest.of(0, 4))
                .stream()
                .map(row -> new HomeMovieResponse(
                        row.getMovieId(),
                        row.getTitle(),
                        row.getSynopsis(),
                        row.getThumbnail(),
                        row.getAdvanceReservationRate()
                ))
                .toList();
    }
}
