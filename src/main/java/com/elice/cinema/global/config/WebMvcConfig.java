package com.elice.cinema.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addArgumentResolvers(java.util.List<org.springframework.web.method.support.HandlerMethodArgumentResolver> resolvers) {
        for (var resolver : resolvers) {
            if (resolver instanceof PageableHandlerMethodArgumentResolver pageableResolver) {
                pageableResolver.setOneIndexedParameters(true);
                return;
            }
        }

        // 기본 resolver가 이미 있으면 그걸 수정하고 없을 때만 새로 추가
        PageableHandlerMethodArgumentResolver fallback = new PageableHandlerMethodArgumentResolver();
        fallback.setOneIndexedParameters(true);
        resolvers.add(fallback);
    }
}
