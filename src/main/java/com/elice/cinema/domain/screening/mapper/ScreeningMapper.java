package com.elice.cinema.domain.screening.mapper;

import com.elice.cinema.domain.screening.dto.response.ScreeningDetailResponse;
import com.elice.cinema.domain.screening.dto.response.ScreeningTimetableResponse;
import com.elice.cinema.domain.screening.entity.Screening;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ScreeningMapper {
    public abstract ScreeningTimetableResponse toScreeningTimetableResponse(Screening screening);
    public abstract ScreeningDetailResponse toScreeningDetailResponse(Screening screening);
}
