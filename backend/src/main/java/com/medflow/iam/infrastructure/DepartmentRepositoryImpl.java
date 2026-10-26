package com.medflow.iam.infrastructure;

import java.util.List;
import java.util.Optional;

import com.medflow.iam.domain.DepartmentInfo;
import com.medflow.iam.domain.DepartmentRepository;
import com.medflow.iam.infrastructure.persistence.SysDepartmentMapper;
import com.medflow.iam.infrastructure.persistence.SysUserMapper;
import com.medflow.iam.infrastructure.persistence.SysDepartmentPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DepartmentRepositoryImpl implements DepartmentRepository {

    private final SysDepartmentMapper mapper;
    private final SysUserMapper userMapper;

    @Override
    public List<DepartmentInfo> findAll() {
        return mapper.findAllDepartments();
    }

    @Override
    public Optional<DepartmentInfo> findById(long id) {
        return mapper.findDepartment(id);
    }

    @Override
    public boolean existsByCode(String code) {
        return mapper.countByCode(code) > 0;
    }

    @Override
    public long insert(String code, String name, Long parentId, String type, int sortNo) {
        SysDepartmentPO po = new SysDepartmentPO();
        po.setCode(code);
        po.setName(name);
        po.setParentId(parentId);
        po.setType(type);
        po.setStatus("ENABLED");
        po.setSortNo(sortNo);
        mapper.insert(po);
        return po.getId();
    }

    @Override
    public void update(long id, String name, Long parentId, String type, int sortNo) {
        SysDepartmentPO po = new SysDepartmentPO();
        po.setId(id);
        po.setName(name);
        po.setParentId(parentId);
        po.setType(type);
        po.setSortNo(sortNo);
        mapper.updateById(po);
    }

    @Override
    public long countUsers(long deptId) {
        return userMapper.countUsersByDept(deptId);
    }
}
