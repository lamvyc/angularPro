package com.medflow.iam.application;

import com.medflow.common.security.DataScope;

public record CreateRoleCommand(String code, String name, DataScope dataScope, String remark) {
}
