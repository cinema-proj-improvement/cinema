package com.elice.cinema.global.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // 400 Bad Request
    MOVIE_INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "MV01", "상영 종료일은 개봉일 이후여야 합니다."),
    MOVIE_THUMBNAIL_REQUIRED(HttpStatus.BAD_REQUEST, "M003", "영화엔 포스터 이미지가 필수입니다."),
    // 401 Unauthorized

    // 403 Forbidden

    // 404 Not Found
    MOVIE_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "영화를 찾을 수 없습니다."),
    // 409 Conflict

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S001", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
