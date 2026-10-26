package com.medflow.iam.application;

import java.util.List;

public record UpdateUserCommand(String realName, Long deptId, String title, String phone, List<Long> roleIds) {
}
