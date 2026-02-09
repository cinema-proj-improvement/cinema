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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.elice.cinema.domain.member.entity.QMember.member;
import static com.elice.cinema.domain.payment.entity.QPayment.payment;
import static com.elice.cinema.domain.reservation.entity.QReservation.reservation;
import static com.elice.cinema.domain.screening.entity.QScreening.screening;
import static com.elice.cinema.domain.reservation.entity.QReservedSeat.reservedSeat;

@Repository
@RequiredArgsConstructor
public class AdminReservationQueryRepositoryCustomImpl implements AdminReservationQueryRepositoryCustom {

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

        List<AdminReservationPageResponse> baseList =
                queryFactory
                        .select(Projections.constructor(
                                AdminReservationPageResponse.class,
                                reservation.id,
                                reservation.reservationCode,
                                reservation.member.name,
                                reservation.status,
                                Expressions.stringTemplate("null"), // seatSummary
                                payment.status,
                                reservation.reservedAt,
                                reservation.totalPrice
                        ))
                        .from(reservation)
                        .leftJoin(payment)
                        .on(payment.reservation.id.eq(reservation.id))
                        .where(condition)
                        .orderBy(reservation.reservedAt.desc())
                        .offset(pageable.getOffset())
                        .limit(pageable.getPageSize())
                        .fetch();

        if (baseList.isEmpty()) {
            return new PageImpl<>(baseList, pageable, 0);
        }

        // 2️⃣ reservationId 목록 추출
        List<Long> reservationIds =
                baseList.stream()
                        .map(AdminReservationPageResponse::getId)
                        .toList();

        // 3️⃣ DB에서 실제 ReservedSeat.seatCode 조회
        Map<Long, String> seatSummaryMap =
                queryFactory
                        .select(
                                reservedSeat.reservation.id,
                                reservedSeat.seatCode
                        )
                        .from(reservedSeat)
                        .where(reservedSeat.reservation.id.in(reservationIds))
                        .orderBy(reservedSeat.id.asc())
                        .fetch()
                        .stream()
                        .collect(Collectors.groupingBy(
                                tuple -> tuple.get(reservedSeat.reservation.id),
                                Collectors.mapping(
                                        tuple -> tuple.get(reservedSeat.seatCode),
                                        Collectors.collectingAndThen(
                                                Collectors.toList(),
                                                list -> String.join(", ", list)
                                        )
                                )
                        ));

        // 4️⃣ seatSummary 반영 (DTO 재생성)
        List<AdminReservationPageResponse> content =
                baseList.stream()
                        .map(r -> new AdminReservationPageResponse(
                                r.getId(),
                                r.getReservationCode(),
                                r.getMemberName(),
                                r.getStatus(),
                                seatSummaryMap.getOrDefault(r.getId(), "-"),
                                r.getPaymentStatus(),
                                r.getReservedAt(),
                                r.getTotalPrice()
                        ))
                        .toList();

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
                                member.email,

                                reservation.movieTitle,
                                reservation.screenName,
                                screening.startAt,

                                seat.seatCode,
                                reservation.totalPrice,
                                payment.status
                        )
                        .from(reservation)
                        .join(reservation.member, member)
                        .join(reservation.screening, screening)
                        .leftJoin(reservedSeat)
                        .on(reservedSeat.reservation.id.eq(reservation.id))
                        .leftJoin(reservedSeat.seat, seat)
                        .leftJoin(payment)
                        .on(payment.reservation.id.eq(reservation.id))
                        .where(reservation.id.eq(reservationId))
                        .orderBy(reservedSeat.id.asc())
                        .fetch();

        if (rows.isEmpty()) {
            return Optional.empty();
        }

        Tuple first = rows.get(0);

        List<String> seatCodes = rows.stream()
                .map(r -> r.get(seat.seatCode))
                .filter(Objects::nonNull)
                .toList();

        LocalDateTime startAt = first.get(screening.startAt);

        return Optional.of(
                new AdminReservationDetailResponse(
                        first.get(reservation.id),
                        first.get(reservation.reservationCode),
                        first.get(reservation.status),
                        first.get(reservation.reservedAt),

                        first.get(reservation.memberName),
                        first.get(member.email),

                        first.get(reservation.movieTitle),
                        first.get(reservation.screenName),
                        startAt.toLocalDate(),
                        startAt.toLocalTime(),

                        seatCodes,
                        seatCodes.size(),

                        first.get(reservation.totalPrice),
                        first.get(payment.status),
                        false   // cancelable → 서비스에서 설정
                )
        );
    }
}
