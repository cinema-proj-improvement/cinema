package com.elice.cinema.domain.screening.mapper;

import com.elice.cinema.domain.screening.dto.reponse.ScreeningDetailResponse;
import com.elice.cinema.domain.screening.dto.reponse.ScreeningTimetableResponse;
import com.elice.cinema.domain.screening.entity.Screening;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ScreeningMapper {
    public abstract ScreeningTimetableResponse toScreeningTimetableResponse(Screening screening);
    public abstract ScreeningDetailResponse toScreeningDetailResponse(Screening screening);
}
