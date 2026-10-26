package com.medflow.iam.infrastructure;

import java.util.List;
import java.util.Optional;

import com.medflow.common.security.DataScope;
import com.medflow.iam.domain.RoleInfo;
import com.medflow.iam.domain.RoleRepository;
import com.medflow.iam.infrastructure.persistence.SysRoleMapper;
import com.medflow.iam.infrastructure.persistence.SysRolePO;
import com.medflow.iam.infrastructure.persistence.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class RoleRepositoryImpl implements RoleRepository {

    private final SysRoleMapper mapper;
    private final SysUserMapper userMapper;

    @Override
    public List<RoleInfo> findAll() {
        return mapper.findAllRoles();
    }

    @Override
    public Optional<RoleInfo> findById(long id) {
        return mapper.findRole(id);
    }

    @Override
    public boolean existsByCode(String code) {
        return mapper.countByCode(code) > 0;
    }

    @Override
    public long insert(String code, String name, DataScope dataScope, String remark) {
        SysRolePO po = new SysRolePO();
        po.setCode(code);
        po.setName(name);
        po.setDataScope(dataScope.name());
        po.setBuiltIn(false);
        po.setStatus("ENABLED");
        po.setRemark(remark);
        mapper.insert(po);
        return po.getId();
    }

    @Override
    public void update(long id, String name, DataScope dataScope, String remark) {
        SysRolePO po = new SysRolePO();
        po.setId(id);
        po.setName(name);
        po.setDataScope(dataScope.name());
        po.setRemark(remark);
        mapper.updateById(po);
    }

    @Override
    public List<Long> findPermissionIds(long roleId) {
        return mapper.findPermissionIds(roleId);
    }

    @Override
    @Transactional
    public void replacePermissions(long roleId, List<Long> permissionIds) {
        mapper.deletePermissions(roleId);
        if (!permissionIds.isEmpty()) {
            mapper.insertPermissions(roleId, permissionIds);
        }
    }

    @Override
    public List<Long> findUserIds(long roleId) {
        return userMapper.findUserIdsByRoleId(roleId);
    }
}
