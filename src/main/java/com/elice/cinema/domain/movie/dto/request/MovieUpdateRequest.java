package com.elice.cinema.domain.movie.dto.request;

import com.elice.cinema.domain.common.ScreeningType;
import com.elice.cinema.domain.movie.entity.AgeRating;
import com.elice.cinema.domain.movie.entity.Genre;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter @Setter
public class MovieUpdateRequest {
    private String title;
    private Integer runningTimeMinutes;
    private AgeRating ageRating;
    private String synopsis;

    private List<Genre> genres;
    private List<ScreeningType> screeningTypes;

    private MultipartFile thumbnailImage;
    private List<MultipartFile> extraImages;

    // 썸네일을 업로드하면 "썸네일 변경"으로 간주
    public boolean isThumbnailChanged() {
        return thumbnailImage != null && !thumbnailImage.isEmpty();
    }

    // extraImages에 파일이 1개라도 있으면 "기타 이미지 변경"으로 간주
    public boolean isExtraChanged() {
        if (extraImages == null) return false;

        for (MultipartFile f : extraImages) {
            if (f != null && !f.isEmpty()) return true;
        }

        return false;
    }

    public boolean hasAnyImageChange() {
        return isThumbnailChanged() || isExtraChanged();
    }
}
