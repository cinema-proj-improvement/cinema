package com.elice.cinema.domain.screen.mapper;

import com.elice.cinema.domain.reservation.dto.response.seatselection.ScreenInfo;
import com.elice.cinema.domain.screen.dto.request.ScreenCreateRequest;
import com.elice.cinema.domain.screen.dto.request.ScreenUpdateRequest;
import com.elice.cinema.domain.screen.dto.response.ScreenDetailResponse;
import com.elice.cinema.domain.screen.dto.response.ScreenListResponse;
import com.elice.cinema.domain.screen.dto.response.ScreenSelectResponse;
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

    // 죄석 선택 페이지에서 필요로 하는 상영관 관련 정보 반환
    default ScreenInfo toScreenInfo(Screen screen) {
        ScreenInfo res = new ScreenInfo();

        res.setScreenId(screen.getId());
        res.setScreenName(screen.getName());
        res.setScreeningType(screen.getScreeningType().getDisplayName());

        return res;
    }

    public abstract ScreenUpdateRequest toScreenUpdateRequest(Screen screen);
    public abstract ScreenListResponse toScreenListResponse(Screen screen);
    public abstract ScreenDetailResponse toScreenDetailResponse(Screen screen);
    public abstract ScreenSelectResponse toScreenSelectResponse(Screen screen);
}
