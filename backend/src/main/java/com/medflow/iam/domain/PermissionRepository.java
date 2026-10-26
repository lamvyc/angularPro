package com.medflow.iam.domain;

import java.util.Collection;
import java.util.List;

public interface PermissionRepository {

    List<PermissionInfo> findAll();

    List<PermissionInfo> findByIds(Collection<Long> ids);
}
