package com.elice.cinema.domain.screen.mapper;

import com.elice.cinema.domain.screen.dto.response.ScreenListResponse;
import com.elice.cinema.domain.screen.entity.Screen;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class ScreenMapper {
    public abstract ScreenListResponse toScreenListResponse(Screen screen);
}
