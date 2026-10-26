package com.medflow.iam.infrastructure;

import java.util.Collection;
import java.util.List;

import com.medflow.iam.domain.PermissionInfo;
import com.medflow.iam.domain.PermissionRepository;
import com.medflow.iam.infrastructure.persistence.SysPermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PermissionRepositoryImpl implements PermissionRepository {

    private final SysPermissionMapper mapper;

    @Override
    public List<PermissionInfo> findAll() {
        return mapper.findAllPermissions();
    }

    @Override
    public List<PermissionInfo> findByIds(Collection<Long> ids) {
        return ids.isEmpty() ? List.of() : mapper.findPermissionsByIds(ids);
    }
}
