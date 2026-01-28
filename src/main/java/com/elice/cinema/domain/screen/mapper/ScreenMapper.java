package com.elice.cinema.domain.screen.mapper;

import com.elice.cinema.domain.screen.dto.request.ScreenCreateRequest;
import com.elice.cinema.domain.screen.dto.response.ScreenDetailResponse;
import com.elice.cinema.domain.screen.dto.response.ScreenListResponse;
import com.elice.cinema.domain.screen.entity.Screen;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ScreenMapper {
    default Screen toEntity(ScreenCreateRequest req) {
        return Screen.of(
                req.getName(),
                req.getScreeningType(),
                req.getTotalSeats(),
                req.getOperating()
        );
    }

    public abstract ScreenListResponse toScreenListResponse(Screen screen);
    public abstract ScreenDetailResponse toScreenDetailResponse(Screen screen);
}
