package com.elice.cinema.domain.payment.service;

import com.elice.cinema.domain.member.entity.Member;
import com.elice.cinema.domain.member.repository.MemberRepository;
import com.elice.cinema.domain.payment.dto.response.TossCancelResponse;
import com.elice.cinema.domain.payment.dto.response.TossConfirmResponse;
import com.elice.cinema.domain.payment.repository.PaymentRepository;
import com.elice.cinema.domain.reservation.entity.Reservation;
import com.elice.cinema.domain.reservation.repository.ReservationRepository;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    @Value("${toss.payments.secret-key}")
    private String tossSecretKey;

    private final PaymentTxService paymentTxService;
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;

    private final RestTemplate restTemplate;

    public void handleSuccess(final String paymentKey, final String orderId, final Long amount, final Long memberId) {
        //중복 호출 시 토스 confirm 호출 방지
        if (paymentRepository.existsByPaymentKey(paymentKey)) {
            return;
        }

        Reservation reservation = reservationRepository.findByReservationCode(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

        Member member = memberRepository.findById(memberId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        validPayment(reservation, amount, member);

        TossConfirmResponse confirmResponse = confirm(paymentKey, orderId, amount);

        if (!"DONE".equals(confirmResponse.getStatus())) {
            throw new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED);
        }

        try {
            paymentTxService.persistPaymentSuccess(confirmResponse, reservation.getId(), member.getId());
        } catch (BusinessException e) {
            rollbackByCancelOrThrow(confirmResponse, reservation.getId(), member.getId(),
                    "결제 검증 실패: " + e.getMessage(),
                    "rollback by business error: " + e.getErrorCode().name());
            throw e;
        } catch (RuntimeException e) {
            rollbackByCancelOrThrow(confirmResponse, reservation.getId(), member.getId(),
                    "서버 처리 실패",
                    "rollback by runtime error");
            throw e;
        }
    }

    // TODO: 토스 응답 요청,응답 시간 정하는 로직 찾아보기
    public TossConfirmResponse confirm(String paymentKey, String orderId, Long amount) {
        String url = "https://api.tosspayments.com/v1/payments/confirm";

        HttpHeaders headers = tossHeaders();

        Map<String, Object> body = Map.of(
                "paymentKey", paymentKey,
                "orderId", orderId,
                "amount", amount
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<TossConfirmResponse> response =
                restTemplate.postForEntity(url, entity, TossConfirmResponse.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED);
        }

        return response.getBody();
    }

    public TossCancelResponse cancel(String paymentKey, String reason) {
        URI uri = UriComponentsBuilder
                .fromUriString("https://api.tosspayments.com")
                .path("/v1/payments/{paymentKey}/cancel")
                .buildAndExpand(paymentKey)
                .toUri();

        HttpHeaders headers = tossHeaders();

        Map<String, Object> body = Map.of(
                "cancelReason", (reason == null || reason.isBlank()) ? "결제 검증 실패" : reason
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<TossCancelResponse> response =
                restTemplate.postForEntity(uri, entity, TossCancelResponse.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new BusinessException(ErrorCode.PAYMENT_CANCEL_FAILED);
        }

        return response.getBody();
    }


    private void validPayment(Reservation reservation, Long amount, Member member) {
        Long totalPrice = reservation.getTotalPrice().longValue();

        // 금액 검증
        if (!totalPrice.equals(amount)) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        // 본인 검증
        if (!reservation.getMember().getId().equals(member.getId())) {
            throw new BusinessException(ErrorCode.PAYMENT_FORBIDDEN);
        }
    }

    private HttpHeaders tossHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String auth = tossSecretKey + ":";
        String encodedAuth = Base64.getEncoder()
                .encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);
        return headers;
    }

    // 결제 취소 실패가 취소 이유를 덮는 것을 방지
    private boolean safeCancel(String paymentKey, String reason) {
        try {
            TossCancelResponse res = cancel(paymentKey, reason);
            return "CANCELED".equals(res.getStatus()) || "PARTIAL_CANCELED".equals(res.getStatus());
        } catch (RuntimeException ex) {
            log.error("cancel failed paymentKey={}, reason={}", paymentKey, reason, ex);
            return false;
        }
    }

    // 승인된 결제를 취소로 롤백하고, 취소 성공/실패를 DB에 남긴다.
    private void rollbackByCancelOrThrow(TossConfirmResponse confirmResponse,
                                         Long reservationId,
                                         Long memberId,
                                         String cancelReason,
                                         String failureMessage) {

        boolean canceled = safeCancel(confirmResponse.getPaymentKey(), cancelReason);

        //TODO: CONFIRM 이후 취소가 되어야 하는 상황에 맞는 취소 페이먼트 생성, 또는 취소를 시도하다 실패한 (취소해야하는) 취소 실패 결제도 추가
        if (canceled) {
            paymentTxService.persistPaymentCanceled(confirmResponse, reservationId, memberId, failureMessage);
            return;
        }

        // 취소 실패는 가장 위험: 반드시 기록하고 사용자에게 결제 취소 실패로 안내
        paymentTxService.persistPaymentCancelFailed(confirmResponse, reservationId, memberId, failureMessage);
        throw new BusinessException(ErrorCode.PAYMENT_CANCEL_FAILED);
    }
}
