package com.elice.cinema.domain.screening.mapper;

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
}
