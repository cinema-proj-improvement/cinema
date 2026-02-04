package com.elice.cinema.domain.payment.repository;

import com.elice.cinema.domain.payment.dto.request.AdminPaymentSearchCondition;
import com.elice.cinema.domain.payment.dto.response.AdminPaymentListMemberResponse;
import com.elice.cinema.domain.payment.dto.response.AdminPaymentListReservationResponse;
import com.elice.cinema.domain.payment.dto.response.AdminPaymentListResponse;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import static com.elice.cinema.domain.member.entity.QMember.member;
import static com.elice.cinema.domain.payment.entity.QPayment.payment;
import static com.querydsl.core.types.Order.ASC;
import static com.querydsl.core.types.Order.DESC;


@Repository
@RequiredArgsConstructor
public class PaymentQueryRepositoryImpl implements PaymentQueryRepository {

    private final JPAQueryFactory queryFactory;

    // 관리자 목록 조회
    @Override
    public Page<AdminPaymentListResponse> findPayments(
            AdminPaymentSearchCondition condition,
            Pageable pageable
    ) {

        // 검색 조건 생성
        BooleanBuilder where = buildWhere(condition);

        // 정렬 조건 생성
        OrderSpecifier<?>[] orderSpecifiers = getOrderBy(condition.getSort());

        // 결제 목록 조회
        List<AdminPaymentListResponse> content =
                queryFactory
                        .select(Projections.constructor(
                                AdminPaymentListResponse.class,
                                payment.id,
                                payment.reservationCode,          // 예매번호
                                payment.amount,                   // 결제 금액
                                payment.status,                   // 결제 상태
                                payment.approvedAt,               // 승인 일시
                                Projections.constructor(
                                        AdminPaymentListMemberResponse.class,
                                        member.name,                // 결제자 이름
                                        member.email                // 결제자 이메일
                                ),
                                Projections.constructor(
                                        AdminPaymentListReservationResponse.class,
                                        payment.reservationCode
                                )
                        ))
                        .from(payment)
                        .join(payment.member, member)
                        .where(where)
                        .orderBy(orderSpecifiers)
                        .offset(pageable.getOffset())
                        .limit(pageable.getPageSize())
                        .fetch();

        // 전체 건수 조회 (페이징 처리용)
        Long total =
                queryFactory
                        .select(payment.count())
                        .from(payment)
                        .join(payment.member, member)
                        .where(where)
                        .fetchOne();

        return new PageImpl<>(
                content,
                pageable,
                total == null ? 0 : total
        );
    }

    // 검색 등
    private BooleanBuilder buildWhere(AdminPaymentSearchCondition condition) {
        BooleanBuilder where = new BooleanBuilder();

        if (condition.getKeyword() != null && !condition.getKeyword().isBlank()) {
            String keyword = condition.getKeyword();

            where.and(
                    payment.reservationCode.containsIgnoreCase(keyword)
                            .or(member.email.containsIgnoreCase(keyword))
            );
        }

        // 결제 상태 필터
        if (condition.getStatus() != null) {
            where.and(payment.status.eq(condition.getStatus()));
        }

        // 승인일 시작
        if (condition.getFromDate() != null) {
            where.and(
                    payment.approvedAt.goe(
                            condition.getFromDate()
                                    .atStartOfDay()
                                    .atOffset(ZoneOffset.UTC)
                    )
            );
        }

        // 승인일 종료
        if (condition.getToDate() != null) {
            where.and(
                    payment.approvedAt.loe(
                            condition.getToDate()
                                    .atTime(LocalTime.MAX)
                                    .atOffset(ZoneOffset.UTC)
                    )
            );
        }

        return where;
    }

    // 정렬
    private OrderSpecifier<?>[] getOrderBy(String sort) {

        // 금액 높은 순
        if ("AMOUNT_DESC".equals(sort)) {
            return new OrderSpecifier[]{
                    new OrderSpecifier<>(DESC, payment.amount),
                    new OrderSpecifier<>(DESC, payment.id)
            };
        }

        // 금액 낮은 순
        if ("AMOUNT_ASC".equals(sort)) {
            return new OrderSpecifier[]{
                    new OrderSpecifier<>(ASC, payment.amount),
                    new OrderSpecifier<>(DESC, payment.id)
            };
        }

        // 기본: 최신순
        return new OrderSpecifier[]{
                payment.approvedAt.desc().nullsLast(),
                payment.id.desc()
        };
    }
}
