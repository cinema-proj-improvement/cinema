package com.elice.cinema.domain.movie.dto.response;

import com.elice.cinema.domain.movie.entity.AgeRating;
import com.elice.cinema.domain.movie.entity.Genre;
import com.elice.cinema.domain.movie.entity.MovieStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class AdminMovieListResponse {

    private Long id;
    private String thumbnail;
    private String title;
    private List<Genre> genres;
    private MovieStatus status;
    private AgeRating ageRating;
    private LocalDate releaseDate;
    private LocalDate endDate;
    private Double avgScore;
    private Double advanceReservationRate;

    /** 단일 row → Response */
    public static AdminMovieListResponse from(AdminMovieJoinRowResponse row) {
        return new AdminMovieListResponse(
                row.getMovieId(),
                row.getThumbnail(),
                row.getTitle(),
                new ArrayList<>(),
                row.getStatus(),
                row.getAgeRating(),
                row.getReleaseDate(),
                row.getEndDate(),
                row.getAvgScore(),
                row.getAdvanceReservationRate()
        );
    }

    /** 장르 누적 */
    public void addGenre(Genre genre) {
        this.genres.add(genre);
    }

    /** 여러 row → Response 리스트 */
    public static List<AdminMovieListResponse> fromRows(
            List<AdminMovieJoinRowResponse> rows
    ) {
        Map<Long, AdminMovieListResponse> map = new LinkedHashMap<>();

        for (AdminMovieJoinRowResponse row : rows) {
            AdminMovieListResponse dto =
                    map.computeIfAbsent(
                            row.getMovieId(),
                            id -> AdminMovieListResponse.from(row)
                    );

            // ⭐ 핵심: 썸네일은 null이면 보정
            if (dto.getThumbnail() == null && row.getThumbnail() != null) {
                dto.setThumbnail(row.getThumbnail());
            }

            if (row.getGenre() != null) {
                dto.addGenre(row.getGenre());
            }
        }

        return new ArrayList<>(map.values());
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public Double getAdvanceReservationRate() {
        if (advanceReservationRate == null) {
            return null;
        }
        return Math.round(advanceReservationRate * 10) / 10.0;
    }
}
