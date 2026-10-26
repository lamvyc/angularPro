package com.medflow.iam.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medflow.iam.domain.DepartmentInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysDepartmentMapper extends BaseMapper<SysDepartmentPO> {

    @Select("""
            SELECT id, code, name, parent_id AS parentId, type, status, sort_no AS sortNo
            FROM sys_department ORDER BY sort_no, id
            """)
    List<DepartmentInfo> findAllDepartments();

    @Select("""
            SELECT id, code, name, parent_id AS parentId, type, status, sort_no AS sortNo
            FROM sys_department WHERE id = #{id}
            """)
    Optional<DepartmentInfo> findDepartment(long id);

    @Select("SELECT COUNT(*) FROM sys_department WHERE code = #{code}")
    int countByCode(String code);
}
