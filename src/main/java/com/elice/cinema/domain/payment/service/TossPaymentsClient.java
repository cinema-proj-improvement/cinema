package com.elice.cinema.domain.payment.service;

import com.elice.cinema.domain.payment.dto.response.TossCancelResponse;
import com.elice.cinema.domain.payment.dto.response.TossConfirmResponse;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import com.elice.cinema.global.error.exception.PaymentFailRedirectException;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class TossPaymentsClient {
    @Value("${toss.payments.secret-key}")
    private String tossSecretKey;

    private final RestTemplate restTemplate;

    public TossConfirmResponse tossConfirm(String paymentKey, String orderId, Long amount) {
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
            throw new PaymentFailRedirectException(ErrorCode.PAYMENT_CONFIRM_FAILED, orderId);
        }

        return response.getBody();
    }

    public TossCancelResponse tossCancel(String paymentKey, long cancelAmount, String reason) {
        URI uri = UriComponentsBuilder
                .fromUriString("https://api.tosspayments.com")
                .path("/v1/payments/{paymentKey}/cancel")
                .buildAndExpand(paymentKey)
                .toUri();

        HttpHeaders headers = tossHeaders();

        Map<String, Object> body = Map.of(
                "cancelAmount", cancelAmount,
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
