package com.medflow.iam;

import java.util.List;

import com.medflow.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class IamSchemaTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void should_apply_v2_iam_migration() {
        Integer version = jdbcTemplate.queryForObject(
                "SELECT MAX(CAST(version AS UNSIGNED)) FROM flyway_schema_history WHERE success = 1",
                Integer.class);

        assertThat(version).isGreaterThanOrEqualTo(2);
    }

    @Test
    void should_seed_five_roles_and_permissions() {
        List<String> roles = jdbcTemplate.queryForList(
                "SELECT code FROM sys_role ORDER BY id", String.class);

        assertThat(roles).containsExactly("ADMIN", "DOCTOR", "NURSE", "PHARMACIST", "CASHIER");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_permission", Integer.class))
                .isGreaterThanOrEqualTo(40);
    }

    @Test
    void should_grant_admin_user_read_permission() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM sys_user u
                JOIN sys_user_role ur ON ur.user_id = u.id
                JOIN sys_role_permission rp ON rp.role_id = ur.role_id
                JOIN sys_permission p ON p.id = rp.permission_id
                WHERE u.username = 'admin' AND p.code = 'system:user:read'
                """, Integer.class);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void should_place_doctor_in_dermatology_department() {
        String deptCode = jdbcTemplate.queryForObject("""
                SELECT d.code
                FROM sys_user u
                JOIN sys_department d ON d.id = u.dept_id
                WHERE u.username = 'doctor1'
                """, String.class);

        assertThat(deptCode).isEqualTo("DERM");
    }

    @Test
    void should_store_bcrypt_password_for_demo_users() {
        String hash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM sys_user WHERE username = 'admin'", String.class);

        assertThat(hash).startsWith("$2");
        assertThat(passwordEncoder.matches("Medflow@2026", hash)).isTrue();
    }

    @Test
    void should_have_login_lock_columns() {
        Integer columns = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'sys_user'
                  AND column_name IN ('failed_attempts', 'locked_until')
                """, Integer.class);

        assertThat(columns).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user", Integer.class)).isEqualTo(8);
    }
}
