package com.medflow.iam.domain;

import java.util.List;
import java.util.Optional;

import com.medflow.common.security.DataScope;

public interface RoleRepository {

    List<RoleInfo> findAll();

    Optional<RoleInfo> findById(long id);

    boolean existsByCode(String code);

    long insert(String code, String name, DataScope dataScope, String remark);

    void update(long id, String name, DataScope dataScope, String remark);

    List<Long> findPermissionIds(long roleId);

    void replacePermissions(long roleId, List<Long> permissionIds);

    List<Long> findUserIds(long roleId);
}
