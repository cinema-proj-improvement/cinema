package com.elice.cinema.global.config;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import org.slf4j.MDC;

// X-Ray 콘솔은 트레이스 ID를 "1-{8자리}-{24자리}" 포맷으로 검색하는데,
// Micrometer가 MDC에 남기는 traceId는 OTel 원본 32자리 hex라서 그대로는 X-Ray에서 검색이 안 된다.
// 같은 트레이스의 모든 Span은 traceId가 동일하므로, 루트 Span이 끝날 때만 MDC를 제거해야
// 자식 Span(JDBC, Redis 등)이 먼저 끝나도 상위 로그에서 xrayTraceId가 사라지지 않는다.
class XRayMdcSpanProcessor implements SpanProcessor {

    private static final String MDC_KEY = "xrayTraceId";

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        MDC.put(MDC_KEY, toXrayTraceId(span.getSpanContext().getTraceId()));
    }

    @Override
    public boolean isStartRequired() {
        return true;
    }

    @Override
    public void onEnd(ReadableSpan span) {
        if (!span.getParentSpanContext().isValid()) {
            MDC.remove(MDC_KEY);
        }
    }

    @Override
    public boolean isEndRequired() {
        return true;
    }

    private String toXrayTraceId(String otelTraceId) {
        return "1-" + otelTraceId.substring(0, 8) + "-" + otelTraceId.substring(8);
    }
}
