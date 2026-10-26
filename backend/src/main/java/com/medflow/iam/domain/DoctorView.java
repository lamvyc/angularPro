package com.medflow.iam.domain;

public record DoctorView(long id, String realName, Long deptId, String deptName, String title) {
}
