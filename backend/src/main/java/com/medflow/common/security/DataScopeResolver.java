package com.medflow.common.security;

import java.util.Optional;

import com.medflow.common.error.BusinessException;
import com.medflow.common.error.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class DataScopeResolver {

    /**
     * 计算科室过滤条件。显式请求越权科室返回 403，而不是静默返回空列表。
     */
    public Optional<Long> resolveDepartmentFilter(Long requestedDeptId) {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        if (user.dataScope() == DataScope.ALL) {
            return Optional.ofNullable(requestedDeptId);
        }
        if (requestedDeptId != null && !user.canAccessDepartment(requestedDeptId)) {
            throw new BusinessException(ErrorCode.AUTHZ_DENIED, "无权访问其他科室数据");
        }
        return Optional.ofNullable(user.deptId());
    }
}
