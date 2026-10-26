package com.medflow.iam.application;

import com.medflow.common.security.DataScope;

public record UpdateRoleCommand(String name, DataScope dataScope, String remark) {
}
