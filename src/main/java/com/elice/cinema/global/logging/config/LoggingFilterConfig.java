package com.elice.cinema.global.logging.config;

import com.elice.cinema.global.logging.filter.MdcLoggingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class LoggingFilterConfig {

    @Bean
    public FilterRegistrationBean<MdcLoggingFilter> mdcLoggingFilter() {
        FilterRegistrationBean<MdcLoggingFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new MdcLoggingFilter());
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);  // 필터 가장 바깥(먼저 실행) 위치
        return bean;
    }
}
