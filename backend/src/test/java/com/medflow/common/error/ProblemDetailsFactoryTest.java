package com.medflow.common.error;

import com.medflow.common.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailsFactoryTest {

    private final ProblemDetailsFactory factory = new ProblemDetailsFactory();

    @Test
    void should_map_error_code_to_problem_detail() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/system/info");
        request.setAttribute(RequestIdFilter.ATTRIBUTE, "req-12345678");

        ProblemDetail problem = factory.create(ErrorCode.AUTH_INVALID, request, null);

        assertThat(problem.getStatus()).isEqualTo(401);
        assertThat(problem.getTitle()).isEqualTo("登录凭据无效");
        assertThat(problem.getDetail()).isEqualTo("用户名或密码错误");
        assertThat(problem.getInstance()).hasToString("/api/v1/system/info");
        assertThat(problem.getProperties())
                .containsEntry("code", "MED-AUTH-001")
                .containsEntry("requestId", "req-12345678");
    }

    @Test
    void should_keep_custom_detail() {
        HttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/appointments");
        ProblemDetail problem = factory.create(ErrorCode.SLOT_FULL, request, "张医生 2026-10-26 上午号源已满");
        assertThat(problem.getDetail()).isEqualTo("张医生 2026-10-26 上午号源已满");
    }
}
