package com.elice.cinema.domain.reservation.repository;

import com.elice.cinema.domain.reservation.dto.response.AdminReservationDetailResponse;
import com.elice.cinema.domain.reservation.dto.response.AdminReservationPageResponse;
import com.elice.cinema.domain.reservation.dto.response.AdminReservationSummaryResponse;
import com.elice.cinema.domain.reservation.entity.QReservation;
import com.elice.cinema.domain.reservation.entity.QReservedSeat;
import com.elice.cinema.domain.reservation.entity.ReservationStatus;
import com.elice.cinema.domain.screen.entity.QSeat;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.elice.cinema.domain.reservation.entity.QReservation.reservation;
import static com.elice.cinema.domain.reservation.entity.QReservedSeat.reservedSeat;

@Repository
@RequiredArgsConstructor
public class ReservationQueryRepositoryImpl implements ReservationQueryRepository {

    private final JPAQueryFactory queryFactory;

    // 관리자 예매 목록 조회
    @Override
    public Page<AdminReservationPageResponse> findAdminReservationPage(
            Long screeningId,
            ReservationStatus status,
            Pageable pageable
    ) {

        BooleanExpression condition =
                reservation.screening.id.eq(screeningId);

        if (status != null) {
            condition = condition.and(reservation.status.eq(status));
        }

        List<AdminReservationPageResponse> content =
                queryFactory
                        .select(Projections.constructor(
                                AdminReservationPageResponse.class,
                                reservation.id,
                                reservation.reservationCode,
                                reservation.member.name,
                                reservation.status,
                                Expressions.stringTemplate(
                                        "group_concat({0})",
                                        reservedSeat.seat.seatCode
                                ),
                                Expressions.constant("PAID"),
                                reservation.reservedAt,
                                reservation.totalPrice
                        ))
                        .from(reservation)
                        .leftJoin(reservedSeat)
                        .on(reservedSeat.reservation.id.eq(reservation.id))
                        .leftJoin(reservedSeat.seat)
                        .where(condition)
                        .groupBy(reservation.id)
                        .orderBy(reservation.reservedAt.desc())
                        .offset(pageable.getOffset())
                        .limit(pageable.getPageSize())
                        .fetch();

        Long total =
                queryFactory
                        .select(reservation.count())
                        .from(reservation)
                        .where(condition)
                        .fetchOne();

        return new PageImpl<>(content, pageable, total);
    }

    // 관리자 예매 요약(상태별 집계)
    @Override
    public AdminReservationSummaryResponse findReservationSummaryByScreening(
            Long screeningId
    ) {
        List<Tuple> results =
                queryFactory
                        .select(
                                reservation.status,
                                reservation.count()
                        )
                        .from(reservation)
                        .where(reservation.screening.id.eq(screeningId))
                        .groupBy(reservation.status)
                        .fetch();

        int confirmed = 0;
        int hold = 0;
        int canceled = 0;

        for (Tuple tuple : results) {
            ReservationStatus status = tuple.get(reservation.status);
            Long count = tuple.get(reservation.count());

            if (status == ReservationStatus.CONFIRMED) {
                confirmed = count.intValue();
            } else if (status == ReservationStatus.HOLD) {
                hold = count.intValue();
            } else if (status == ReservationStatus.CANCELED) {
                canceled = count.intValue();
            }
        }

        int total = confirmed + hold + canceled;

        return new AdminReservationSummaryResponse(
                total,
                confirmed,
                hold,
                canceled
        );
    }

    // 관리자 예매 상세 조회
    @Override
    public Optional<AdminReservationDetailResponse> findAdminDetailById(Long reservationId) {

        QReservation reservation = QReservation.reservation;
        QReservedSeat reservedSeat = QReservedSeat.reservedSeat;
        QSeat seat = QSeat.seat;

        List<Tuple> rows =
                queryFactory
                        .select(
                                reservation.id,
                                reservation.reservationCode,
                                reservation.status,
                                reservation.reservedAt,
                                reservation.memberName,
                                reservation.movieTitle,
                                reservation.screenName,
                                seat.seatCode,
                                reservation.totalPrice,
                                reservation.status.stringValue(),
                                reservation.status.eq(ReservationStatus.CONFIRMED)
                        )
                        .from(reservation)
                        .leftJoin(reservedSeat)
                        .on(reservedSeat.reservation.id.eq(reservation.id))
                        .leftJoin(reservedSeat.seat, seat)
                        .where(reservation.id.eq(reservationId))
                        .fetch();

        if (rows.isEmpty()) {
            return Optional.empty();
        }

        // 첫 row = 공통 데이터
        Tuple first = rows.get(0);

        List<String> seatCodes = rows.stream()
                .map(r -> r.get(seat.seatCode))
                .filter(Objects::nonNull)
                .toList();

        return Optional.of(
                new AdminReservationDetailResponse(
                        first.get(reservation.id),
                        first.get(reservation.reservationCode),
                        first.get(reservation.status),
                        first.get(reservation.reservedAt),
                        first.get(reservation.memberName),
                        "", // memberLoginId (임시)
                        first.get(reservation.movieTitle),
                        first.get(reservation.screenName),
                        null,
                        null,
                        seatCodes,
                        0, // seatCount → Mapper에서 채움
                        first.get(reservation.totalPrice),
                        first.get(reservation.status.stringValue()),
                        first.get(reservation.status.eq(ReservationStatus.CONFIRMED))
                )
        );
    }
}
