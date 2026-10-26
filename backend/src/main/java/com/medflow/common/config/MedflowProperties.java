package com.medflow.common.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 应用级配置。所有环境相关配置通过环境变量覆盖，不在代码中写死。
 *
 * @param web Web/HTTP 相关配置
 */
@ConfigurationProperties(prefix = "medflow")
public record MedflowProperties(Web web) {

    public MedflowProperties {
        if (web == null) {
            web = new Web(List.of("http://localhost:4200"));
        }
    }

    /**
     * @param allowedOrigins 允许的跨域来源（开发期为 Angular dev server）
     */
    public record Web(List<String> allowedOrigins) {

        public Web {
            allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
        }
    }
}
