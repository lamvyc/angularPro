package com.medflow.iam.domain;

public record UserQuery(int page, int size, String keyword, Long deptId, String status) {
}
