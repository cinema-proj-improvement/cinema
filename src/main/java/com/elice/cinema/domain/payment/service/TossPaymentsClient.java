package com.elice.cinema.domain.payment.service;

import com.elice.cinema.domain.payment.dto.response.TossCancelResponse;
import com.elice.cinema.domain.payment.dto.response.TossConfirmResponse;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import com.elice.cinema.global.error.exception.PaymentFailRedirectException;
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
public class TossPaymentsClient {
    @Value("${toss.payments.secret-key}")
    private String tossSecretKey;

    private final RestTemplate restTemplate;

    public TossConfirmResponse tossConfirm(String paymentKey, String orderId, Long amount) {
        String url = "https://api.tosspayments.com/v1/payments/confirm";

        log.info("[Toss] 결제 승인 요청: orderId={}, amount={}", orderId, amount);

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
            log.error("[Toss] 결제 승인 실패: orderId={}, statusCode={}", orderId, response.getStatusCode());
            throw new PaymentFailRedirectException(ErrorCode.PAYMENT_CONFIRM_FAILED, orderId);
        }

        log.info("[Toss] 결제 승인 성공: orderId={}, amount={}", orderId, amount);
        return response.getBody();
    }

    public TossCancelResponse tossCancel(String paymentKey, long cancelAmount, String reason) {
        String cancelReason = (reason == null || reason.isBlank()) ? "결제 검증 실패" : reason;

        log.info("[Toss] 결제 취소 요청: paymentKey={}, cancelAmount={}, reason={}", paymentKey, cancelAmount, cancelReason);

        URI uri = UriComponentsBuilder
                .fromUriString("https://api.tosspayments.com")
                .path("/v1/payments/{paymentKey}/cancel")
                .buildAndExpand(paymentKey)
                .toUri();

        HttpHeaders headers = tossHeaders();

        Map<String, Object> body = Map.of(
                "cancelAmount", cancelAmount,
                "cancelReason", cancelReason
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<TossCancelResponse> response =
                restTemplate.postForEntity(uri, entity, TossCancelResponse.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            log.error("[Toss] 결제 취소 실패: paymentKey={}, statusCode={}", paymentKey, response.getStatusCode());
            throw new BusinessException(ErrorCode.PAYMENT_CANCEL_FAILED);
        }

        log.info("[Toss] 결제 취소 성공: paymentKey={}, cancelAmount={}", paymentKey, cancelAmount);
        return response.getBody();
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
}
