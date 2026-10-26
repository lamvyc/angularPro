package com.medflow.common.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class TimeConfig {

    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    /**
     * 统一业务时钟。测试通过 {@code @TestConfiguration} 覆盖为固定时钟。
     */
    @Bean
    public Clock businessClock() {
        return Clock.system(BUSINESS_ZONE);
    }
}
