package com.medflow.system.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import com.medflow.system.domain.SystemInfo;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class SystemAppService {

    private final Environment environment;
    private final Clock clock;

    public SystemAppService(Environment environment, Clock clock) {
        this.environment = environment;
        this.clock = clock;
    }

    public SystemInfo info() {
        List<String> profiles = Arrays.asList(environment.getActiveProfiles());
        return new SystemInfo(
                environment.getProperty("spring.application.name", "medflow-api"),
                environment.getProperty("spring.application.version", "0.1.0-SNAPSHOT"),
                profiles,
                Instant.now(clock));
    }

    public Instant now() {
        return Instant.now(clock);
    }
}
