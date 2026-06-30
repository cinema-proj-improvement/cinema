package com.elice.cinema.domain.movieImage.dto;

import java.util.ArrayList;
import java.util.List;

public record MovieImageUploadResult(String thumbnailKey, List<String> extraKeys) {

    public List<String> getAllKeys() {
        List<String> all = new ArrayList<>();
        if (thumbnailKey != null) all.add(thumbnailKey);
        all.addAll(extraKeys);
        return all;
    }
}
