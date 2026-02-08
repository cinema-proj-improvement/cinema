package com.elice.cinema.domain.screening.repository;

import com.elice.cinema.domain.reservation.entity.ReservationStatus;
import com.elice.cinema.domain.screening.dto.request.AdminScreeningSearchRequest;
import com.elice.cinema.domain.screening.dto.response.AdminScreeningFilterOptionResponse;
import com.elice.cinema.domain.screening.dto.response.AdminScreeningSeatResponse;
import com.elice.cinema.domain.screening.dto.response.AdminScreeningSeatSummaryResponse;
import com.elice.cinema.domain.screening.entity.Screening;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static com.elice.cinema.domain.movie.entity.QMovie.movie;
import static com.elice.cinema.domain.reservation.entity.QReservation.reservation;
import static com.elice.cinema.domain.reservation.entity.QReservedSeat.reservedSeat;
import static com.elice.cinema.domain.screen.entity.QScreen.screen;
import static com.elice.cinema.domain.screen.entity.QSeat.seat;
import static com.elice.cinema.domain.screening.entity.QScreening.screening;
import static org.springframework.util.StringUtils.hasText;


@RequiredArgsConstructor
public class ScreeningRepositoryImpl implements ScreeningRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Screening> searchAdmin(
            AdminScreeningSearchRequest request,
            Pageable pageable
    ) {
        BooleanExpression whereClause = adminConditions(request);

        List<Screening> contents = queryFactory
                .selectFrom(screening)
                .join(screening.movie, movie).fetchJoin()
                .join(screening.screen, screen).fetchJoin()
                .where(whereClause)
                .orderBy(screening.startAt.desc())   // 관리자 기준
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long count = queryFactory
                .select(screening.count())
                .from(screening)
                .where(whereClause)
                .fetchOne();

        return new PageImpl<>(
                contents,
                pageable,
                count == null ? 0 : count
        );
    }

    @Override
    public List<AdminScreeningFilterOptionResponse> findAdminScreeningMovieFilterOptions() {
        return queryFactory
                .select(Projections.constructor(
                        AdminScreeningFilterOptionResponse.class,
                        movie.id,                                // movieId
                        movie.title,                             // movieName
                        Expressions.nullExpression(Long.class),  // screenId
                        Expressions.nullExpression(String.class) // screenName
                ))
                .from(screening)
                .join(screening.movie, movie)
                .groupBy(movie.id, movie.title)
                .fetch();
    }

    @Override
    public List<AdminScreeningFilterOptionResponse> findAdminScreeningScreenFilterOptions() {
        return queryFactory
                .select(Projections.constructor(
                        AdminScreeningFilterOptionResponse.class,
                        Expressions.nullExpression(Long.class),  // movieId
                        Expressions.nullExpression(String.class),// movieName
                        screen.id,                               // screenId
                        screen.name                              // screenName
                ))
                .from(screening)
                .join(screening.screen, screen)
                .groupBy(screen.id, screen.name)
                .fetch();
    }


    private BooleanExpression adminConditions(AdminScreeningSearchRequest r) {
        return dateBetween(r)
                .and(movieEq(r))
                .and(screenEq(r))
                .and(keywordContains(r));
    }

    // 날짜 범위 필터
    private BooleanExpression dateBetween(AdminScreeningSearchRequest r) {
        if (r.getStartDate() == null || r.getEndDate() == null) {
            return null;
        }

        LocalDateTime start = r.getStartDate().atStartOfDay();
        LocalDateTime end = r.getEndDate().plusDays(1).atStartOfDay();

        return screening.startAt.between(start, end);
    }

    // 영화 ID 필터
    private BooleanExpression movieEq(AdminScreeningSearchRequest r) {
        return r.getMovieId() == null
                ? null
                : screening.movie.id.eq(r.getMovieId());
    }

    // 상영관 필터
    private BooleanExpression screenEq(AdminScreeningSearchRequest r) {
        return r.getScreenId() == null
                ? null
                : screening.screen.id.eq(r.getScreenId());
    }

    // 영화 제목 검색
    private BooleanExpression keywordContains(AdminScreeningSearchRequest r) {
        return hasText(r.getKeyword())
                ? screening.movie.title.containsIgnoreCase(r.getKeyword())
                : null;
    }

    // 상영별 좌석 상태 조회
    @Override
    public List<AdminScreeningSeatResponse> findAdminSeatsByScreeningId(Long screeningId) {

        return queryFactory
                .select(Projections.constructor(
                        AdminScreeningSeatResponse.class,
                        seat.id,
                        seat.seatCode,
                        seat.rowNo,
                        seat.colNo,
                        new CaseBuilder()
                                .when(seat.active.isFalse())
                                .then(Expressions.constant("INACTIVE"))
                                .when(reservation.status.eq(ReservationStatus.CONFIRMED))
                                .then(Expressions.constant("CONFIRMED"))
                                .when(reservation.status.eq(ReservationStatus.HOLD))
                                .then(Expressions.constant("HOLD"))
                                .otherwise(Expressions.constant("AVAILABLE"))
                ))
                .from(seat)
                .join(screening).on(screening.screen.eq(seat.screen))
                .leftJoin(reservedSeat).on(
                        reservedSeat.seat.eq(seat),
                        reservedSeat.screening.id.eq(screeningId)
                )
                .leftJoin(reservation).on(reservedSeat.reservation.eq(reservation))
                .where(screening.id.eq(screeningId))
                .fetch();
    }

    // 상영별 좌석 요약 조회(상세페이지)
    public AdminScreeningSeatSummaryResponse findAdminSeatSummaryByScreeningId(Long screeningId) {

        Long total = queryFactory
                .select(seat.count())
                .from(seat)
                .join(screening).on(screening.screen.eq(seat.screen))
                .where(
                        screening.id.eq(screeningId),
                        seat.active.isTrue()
                )
                .fetchOne();

        Long confirmed = queryFactory
                .select(reservedSeat.count())
                .from(reservedSeat)
                .join(reservedSeat.reservation, reservation)
                .where(
                        reservedSeat.screening.id.eq(screeningId),
                        reservation.status.eq(ReservationStatus.CONFIRMED)
                )
                .fetchOne();

        Long hold = queryFactory
                .select(reservedSeat.count())
                .from(reservedSeat)
                .join(reservedSeat.reservation, reservation)
                .where(
                        reservedSeat.screening.id.eq(screeningId),
                        reservation.status.eq(ReservationStatus.HOLD)
                )
                .fetchOne();

        long t = total == null ? 0 : total;
        long c = confirmed == null ? 0 : confirmed;
        long h = hold == null ? 0 : hold;

        return new AdminScreeningSeatSummaryResponse(
                t,
                c,
                h,
                t - c - h
        );
    }

    // 좌석 현황
    @Override
    public List<Tuple> findSeatSummaryByScreeningIds(List<Long> screeningIds) {

        NumberExpression<Long> confirmedCount =
                new CaseBuilder()
                        .when(reservation.status.eq(ReservationStatus.CONFIRMED)).then(1L)
                        .otherwise(0L)
                        .sum();

        NumberExpression<Long> holdCount =
                new CaseBuilder()
                        .when(reservation.status.eq(ReservationStatus.HOLD)).then(1L)
                        .otherwise(0L)
                        .sum();

        return queryFactory
                .select(
                        screening.id,
                        seat.countDistinct(),
                        confirmedCount,
                        holdCount
                )
                .from(screening)
                .join(seat).on(seat.screen.eq(screening.screen))
                .leftJoin(reservedSeat).on(
                        reservedSeat.screening.eq(screening),
                        reservedSeat.seat.eq(seat)
                )
                .leftJoin(reservation).on(reservedSeat.reservation.eq(reservation))
                .where(
                        screening.id.in(screeningIds),
                        seat.active.isTrue()
                )
                .groupBy(screening.id)
                .fetch();
    }
}
