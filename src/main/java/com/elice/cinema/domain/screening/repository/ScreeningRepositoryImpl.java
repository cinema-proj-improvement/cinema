package com.elice.cinema.domain.screening.repository;

import com.elice.cinema.domain.screening.dto.request.AdminScreeningSearchRequest;
import com.elice.cinema.domain.screening.entity.Screening;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static com.elice.cinema.domain.movie.entity.QMovie.movie;
import static com.elice.cinema.domain.screen.entity.QScreen.screen;
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
                .orderBy(screening.createdAt.desc())   // 관리자 기준
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
}
