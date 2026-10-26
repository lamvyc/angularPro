package com.medflow.system.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import com.medflow.system.domain.SystemInfo;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class SystemAppServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-10-26T01:30:00Z");

    @Test
    void should_expose_application_info_and_fixed_clock() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.application.name", "medflow-api")
                .withProperty("spring.application.version", "9.9.9")
                .withProperty("spring.profiles.active", "test");
        Clock clock = Clock.fixed(FIXED_NOW, ZoneId.of("Asia/Shanghai"));

        SystemAppService service = new SystemAppService(environment, clock);
        SystemInfo info = service.info();

        assertThat(info.application()).isEqualTo("medflow-api");
        assertThat(info.version()).isEqualTo("9.9.9");
        assertThat(info.serverTime()).isEqualTo(FIXED_NOW);
        assertThat(service.now()).isEqualTo(FIXED_NOW);
    }
}
