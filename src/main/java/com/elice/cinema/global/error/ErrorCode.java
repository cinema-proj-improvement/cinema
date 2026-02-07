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
    SCREEN_UPDATE_NOT_ALLOWED_WHEN_SCREENING_ACTIVE(HttpStatus.BAD_REQUEST, "SC07", "활성 상영이 있는 상영관은 수정할 수 없습니다."),
    SEAT_UPDATE_NOT_ALLOWED_WHEN_SCREENING_NOT_FINISHED(HttpStatus.BAD_REQUEST, "SC08", "종료되지 않은 상영이 있는 상영관의 좌석은 수정할 수 없습니다."),
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
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "PY01", "결제 금액이 일치하지 않습니다."),
    PAYMENT_CONFIRM_FAILED(HttpStatus.BAD_REQUEST, "PY02", "결제 승인에 실패했습니다."),
    PAYMENT_CANCEL_FAILED(HttpStatus.BAD_REQUEST, "PY05", "결제 취소에 실패했습니다. 관리자에게 문의하세요."),
    PAYMENT_CANCELED_AFTER_CONFIRM(HttpStatus.BAD_REQUEST, "PY06", "결제 처리 중 오류가 발생하여 결제가 취소되었습니다."),
    PAYMENT_CANCELED_FAILED_AFTER_CONFIRM(HttpStatus.INTERNAL_SERVER_ERROR, "PY07", "결제 처리 중 오류가 발생했으며, 결제 취소에도 실패했습니다. 고객센터에 문의해주세요."),

    SCREENING_DELETE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "SG10", "SCHEDULED 상태의 상영만 삭제 가능합니다."),
    SEAT_INACTIVE(HttpStatus.BAD_REQUEST, "ST06", "사용 불가능한 좌석입니다."),
    SEAT_ALREADY_HELD(HttpStatus.BAD_REQUEST, "ST07", "이미 다른 사람이 점유한 좌석입니다."),

    RESERVATION_SEAT_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "RS04", "한 번에 예매할 수 있는 좌석 수를 초과했습니다."),
    RESERVATION_NOT_CANCELABLE(HttpStatus.BAD_REQUEST, "RS02", "취소할 수 없는 예매 상태입니다."),
    RESERVATION_ALREADY_CANCELED(HttpStatus.BAD_REQUEST, "RS03", "이미 취소된 예매입니다."),
    MOVIE_SORT_NOT_SUPPORTED(HttpStatus.BAD_REQUEST,"MV06", "해당 정렬 기능은 현재 지원되지 않습니다."),

    RESERVATION_ALREADY_CONFIRMED(HttpStatus.BAD_REQUEST, "RS06", "이미 확정된 예매입니다."),

    REFUND_POLICY_INVALID_RATE(HttpStatus.BAD_REQUEST, "PL01", "환불 퍼센트는 0~100 사이여야 합니다."),
    REFUND_POLICY_INVALID_BEFORE_TIME(HttpStatus.BAD_REQUEST, "PL02", "상영 시작 전 시간은 0 이상이어야 합니다."),
    REFUND_POLICY_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "PL03", "환불 정책명은 필수입니다."),



    REFUND_POLICY_NOT_FOUND(HttpStatus.BAD_REQUEST, "RF01", "적용 가능한 환불 정책이 없습니다."),
    REFUND_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "RF03", "현재 시점에서는 환불이 불가능합니다."),
    INVALID_REFUND_RATE(HttpStatus.BAD_REQUEST, "RF04", "환불 비율이 올바르지 않습니다."),
    INVALID_PAYMENT_AMOUNT(HttpStatus.BAD_REQUEST, "RF05", "결제 금액이 올바르지 않습니다."),

    // 401 Unauthorized


    // 403 Forbidden
    PAYMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "PY04", "결제 권한이 없습니다."),
    RESERVATION_FORBIDDEN(HttpStatus.FORBIDDEN, "RS05", "해당 예매 권한이 없습니다."),

    // 404 Not Found
    MOVIE_NOT_FOUND(HttpStatus.NOT_FOUND, "MV01", "영화를 찾을 수 없습니다."),
    SCREEN_NOT_FOUND(HttpStatus.NOT_FOUND, "SC05", "상영관을 찾을 수 없습니다."),
    SEAT_NOT_FOUND(HttpStatus.NOT_FOUND, "ST05", "좌석을 찾을 수 없습니다."),
    SCREENING_NOT_FOUND(HttpStatus.NOT_FOUND, "SG11", "상영을 찾을 수 없습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MB04", "사용자를 찾을 수 없습니다."),
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "RS01", "예약 정보를 찾을 수 없습니다."),
    MOVIE_THUMBNAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "MV05", "영화 썸네일 정보를 찾을 수 없습니다."),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PY03", "결제 정보를 찾을 수 없습니다."),
    PAYMENT_CANCEL_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "PM02", "현재 결제 상태에서는 취소할 수 없습니다."),

    // 409 Conflict
    SCREEN_NAME_DUPLICATED(HttpStatus.CONFLICT, "SC06", "이미 존재하는 상영관 이름입니다."),
    SCREENING_TIME_CONFLICT(HttpStatus.CONFLICT, "SG12", "해당 시간에 이미 등록된 상영이 있어 상영을 생성할 수 없습니다."),
    REFUND_POLICY_DUPLICATED_BEFORE_TIME(HttpStatus.CONFLICT, "PL04", "이미 동일한 기준 시간의 환불 정책이 존재합니다."),
    REFUND_ALREADY_EXISTS(HttpStatus.CONFLICT, "RF02", "이미 환불이 처리된 결제입니다."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SE01", "서버 내부 오류가 발생했습니다."),

    // 501 Not Implemented
    ADMIN_FEATURE_NOT_IMPLEMENTED(HttpStatus.NOT_IMPLEMENTED, "SE02", "해당 관리자 기능은 아직 구현되지 않았습니다."),
    FEATURE_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "SE03", "해당 기능은 현재 지원되지 않습니다."),


    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "IO01", "파일 업로드에 실패했습니다."),
    ENVIRONMENT_POLICY_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "PL05", "시스템 환경 정책이 설정되지 않았습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
