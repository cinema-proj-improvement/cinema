package com.elice.cinema.domain.movie.event;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record MovieImagesStorageEvent(
        Long movieId,
        MultipartFile thumbnailImage,
        List<MultipartFile> extraImages
) {
    public static MovieImagesStorageEvent of(Long movieId, MultipartFile thumbnailImage, List<MultipartFile> extraImages) {
        return new MovieImagesStorageEvent(movieId, thumbnailImage, extraImages);
    }
}