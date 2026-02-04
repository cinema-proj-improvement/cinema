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
    SCREENING_START_AT_REQUIRED(HttpStatus.BAD_REQUEST, "SG01", "상영 시작 시간은 필수입니다."),
    SCREENING_BEFORE_RELEASE_DATE(HttpStatus.BAD_REQUEST, "SG02", "상영 시작일은 영화 개봉일 이후여야 합니다."),
    SCREENING_AFTER_END_DATE(HttpStatus.BAD_REQUEST, "SG03", "상영 시작일은 영화 상영 종료일 이전이어야 합니다."),
    SCREENING_TYPE_NOT_SUPPORTED_BY_MOVIE(HttpStatus.BAD_REQUEST, "SG04", "해당 영화가 지원하지 않는 상영 타입입니다."),
    SCREENING_TYPE_NOT_MATCH_SCREEN(HttpStatus.BAD_REQUEST, "SG05", "상영관의 상영 타입과 선택한 상영 타입이 일치하지 않습니다."),
    SCREENING_ALREADY_ENDED(HttpStatus.BAD_REQUEST, "SG06", "이미 종료된 상영은 생성할 수 없습니다."),
    SCREENING_INVALID_STATUS(HttpStatus.BAD_REQUEST, "SG07", "상영 상태를 결정할 수 없습니다."),
    SCREENING_STATUS_CHANGE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "SG08", "SCHEDULED 상태의 상영만 상태 변경이 가능합니다."),
    SCREENING_ONLY_CAN_CANCEL(HttpStatus.BAD_REQUEST, "SG09", "상영 상태는 CANCELED로만 변경할 수 있습니다."),
    MEMBER_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "MB01", "비밀번호 확인이 일치하지 않습니다."),
    MEMBER_EMAIL_DUPLICATED(HttpStatus.BAD_REQUEST, "MB02", "이미 사용 중인 이메일입니다."),
    MEMBER_NICKNAME_DUPLICATED(HttpStatus.BAD_REQUEST, "MB03", "이미 사용 중인 닉네임입니다."),
    SCREENING_DELETE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "SG10", "SCHEDULED 상태의 상영만 삭제 가능합니다."),
    SEAT_INACTIVE(HttpStatus.BAD_REQUEST, "ST06", "사용 불가능한 좌석입니다."),
    SEAT_ALREADY_HELD(HttpStatus.BAD_REQUEST, "ST07", "이미 다른 사람이 점유한 좌석입니다."),
    RESERVATION_SEAT_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "RV01", "한 번에 예매할 수 있는 좌석 수를 초과했습니다."),
    // 401 Unauthorized


    // 403 Forbidden

    // 404 Not Found
    MOVIE_NOT_FOUND(HttpStatus.NOT_FOUND, "MV01", "영화를 찾을 수 없습니다."),
    SCREEN_NOT_FOUND(HttpStatus.NOT_FOUND, "SC05", "상영관을 찾을 수 없습니다."),
    SEAT_NOT_FOUND(HttpStatus.NOT_FOUND, "ST05", "좌석을 찾을 수 없습니다."),
    SCREENING_NOT_FOUND(HttpStatus.NOT_FOUND, "SG11", "상영을 찾을 수 없습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MB04", "사용자를 찾을 수 없습니다."),

    // 409 Conflict
    SCREEN_NAME_DUPLICATED(HttpStatus.CONFLICT, "SC06", "이미 존재하는 상영관 이름입니다."),
    SCREENING_TIME_CONFLICT(HttpStatus.CONFLICT, "SG12", "해당 시간에 이미 등록된 상영이 있어 상영을 생성할 수 없습니다."),

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
