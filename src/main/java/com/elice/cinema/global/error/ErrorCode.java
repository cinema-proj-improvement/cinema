package com.elice.cinema.global.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // 400 Bad Request
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "BR01", "잘못된 요청입니다."),
    MOVIE_INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "MV02", "상영 종료일은 개봉일 이후여야 합니다."),
    MOVIE_THUMBNAIL_REQUIRED(HttpStatus.BAD_REQUEST, "MV03", "영화엔 포스터 이미지가 필수입니다."),
    MOVIE_RUNNING_TIME_CANNOT_CHANGE_WHEN_SCREENING_EXISTS(HttpStatus.BAD_REQUEST, "MV04", "상영중인 회차가 존재하여 러닝타임을 수정할 수 없습니다."),
    SCREEN_SEAT_REQUIRED(HttpStatus.BAD_REQUEST, "SC01", "좌석 정보는 최소 1개 이상 필요합니다."),
    SCREEN_SEAT_COUNT_MISMATCH(HttpStatus.BAD_REQUEST, "SC02", "총 좌석 수와 좌석 정보 수가 일치하지 않습니다."),
    SCREEN_DUPLICATE_SEAT_POSITION(HttpStatus.BAD_REQUEST, "SC03", "중복된 좌석 위치(row/col)가 존재합니다."),
    SCREEN_DUPLICATE_SEAT_CODE(HttpStatus.BAD_REQUEST, "SC04", "중복된 좌석 코드가 존재합니다."),

    // 401 Unauthorized

    // 403 Forbidden

    // 404 Not Found
    MOVIE_NOT_FOUND(HttpStatus.NOT_FOUND, "MV01", "영화를 찾을 수 없습니다."),
    SCREEN_NOT_FOUND(HttpStatus.NOT_FOUND, "SC05", "상영관을 찾을 수 없습니다."),

    // 409 Conflict
    SCREEN_NAME_DUPLICATED(HttpStatus.CONFLICT, "SC06", "이미 존재하는 상영관 이름입니다."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SE01", "서버 내부 오류가 발생했습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "IO01", "파일 업로드에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
