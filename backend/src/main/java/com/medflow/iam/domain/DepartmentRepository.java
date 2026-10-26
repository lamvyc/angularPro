package com.medflow.iam.domain;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository {

    List<DepartmentInfo> findAll();

    Optional<DepartmentInfo> findById(long id);

    boolean existsByCode(String code);

    long insert(String code, String name, Long parentId, String type, int sortNo);

    void update(long id, String name, Long parentId, String type, int sortNo);

    long countUsers(long deptId);
}
