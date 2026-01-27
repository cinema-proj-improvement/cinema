package com.elice.cinema.global.common.file;

public enum FileCategory {
    MOVIE_THUMBNAIL("movies/thumbnails"),
    MOVIE_EXTRA("movies/extras");

    private final String dir;

    FileCategory(String dir) {
        this.dir = dir;
    }

    public String getDir() {
        return dir;
    }
}
