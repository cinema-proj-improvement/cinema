package com.elice.cinema.domain.screening.mapper;

import com.elice.cinema.domain.reservation.dto.response.seatselection.ScreeningInfo;
import com.elice.cinema.domain.screening.dto.response.*;
import com.elice.cinema.domain.screening.entity.Screening;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Mapper(componentModel = "spring")
public interface ScreeningMapper {
    public abstract ScreeningTimetableResponse toScreeningTimetableResponse(Screening screening);
    public abstract ScreeningDetailResponse toScreeningDetailResponse(Screening screening);
    @Mapping(
            target = "screen.screeningTypeDisplayName",
            expression = "java(screen.getScreeningType().getDisplayName())"
    )
    public abstract ReservationScheduleResponse toReservationScheduleResponse(Screening screening, Integer remainingSeats);

    // 매퍼는 필드연결만 해주고 시간같은 의미 변환은 직접 알려줘야 한다.
    @Mapping(target = "date", source = "startAt", qualifiedByName = "toDate")
    @Mapping(target = "startTime", source = "startAt", qualifiedByName = "toStartTime")
    @Mapping(target = "endTime", source = "endAt", qualifiedByName = "toEndTime")
    @Mapping(target = "seatSummary", ignore = true)
    AdminScreeningResponse toAdminListResponse(Screening screening);

    // LocalDateTime → LocalDate / LocalTime 변환 메서드 (날짜 시간 분해를 자동으로 하지 않기에 변환 메서드 필수)
    @Named("toDate")
    default LocalDate mapDate(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalDate();
    }

    @Named("toStartTime")
    default LocalTime mapStartTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalTime();
    }

    @Named("toEndTime")
    default LocalTime mapEndTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalTime();
    }

    // 좌석 선택 페이지에서 필요로 하는 상영 관련 정보 반환
    default ScreeningInfo toScreeningInfo(Screening screening) {
        ScreeningInfo res = new ScreeningInfo();

        res.setScreeningId(screening.getId());
        res.setStartAt(screening.getStartAt());
        res.setEndAt(screening.getEndAt());
        res.setMovieTitle(screening.getMovie().getTitle());  // 호출 usecase 위치에서 fetch join으로 가져옴

        return res;
    }

    default AdminScreeningResponse toAdminListResponse(
            Screening screening,
            AdminScreeningSeatSummaryResponse seatSummary
    ) {
        AdminScreeningResponse base = toAdminListResponse(screening);

        return new AdminScreeningResponse(
                base.getId(),
                base.getDate(),
                base.getStartTime(),
                base.getEndTime(),
                base.getMovie(),
                base.getScreen(),
                base.getScreeningType(),
                base.getScreeningStatus(),
                seatSummary
        );
    }
}
