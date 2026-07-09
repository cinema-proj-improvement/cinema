package com.elice.cinema.global.config;

import io.opentelemetry.contrib.awsxray.AwsXrayIdGenerator;
import org.springframework.boot.actuate.autoconfigure.tracing.SdkTracerProviderBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// X-Ray는 자체 Trace ID 포맷(1-{8자리 타임스탬프}-{24자리 랜덤})을 쓰기 때문에,
// 표준 OTel Trace ID 생성기 대신 이 IdGenerator를 등록해야 X-Ray가 트레이스를 정상적으로 인식한다.
@Configuration
public class XRayTracingConfig {

    @Bean
    public SdkTracerProviderBuilderCustomizer xrayIdGeneratorCustomizer() {
        return builder -> builder
                .setIdGenerator(AwsXrayIdGenerator.getInstance())
                .addSpanProcessor(new XRayMdcSpanProcessor());
    }
}
