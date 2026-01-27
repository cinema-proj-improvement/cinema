package com.elice.cinema.global.config;

import com.elice.cinema.global.config.properties.FileProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    private final FileProperties props;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (!"local".equalsIgnoreCase(props.getStorage().getType())) {
            return;
        }

        String basePath = props.getUpload().getBasePath();
        String urlPrefix = props.getUpload().getUrlPrefix();

        String location = "file:" + (basePath.endsWith("/") ? basePath : basePath + "/");

        registry.addResourceHandler(urlPrefix + "/**")
                .addResourceLocations(location);
    }

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
