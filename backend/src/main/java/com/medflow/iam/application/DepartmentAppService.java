package com.medflow.iam.application;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.medflow.common.error.BusinessException;
import com.medflow.common.error.ErrorCode;
import com.medflow.iam.domain.DepartmentInfo;
import com.medflow.iam.domain.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DepartmentAppService {

    private static final Set<String> TYPES = Set.of("CLINICAL", "PHARMACY", "ADMIN");

    private final DepartmentRepository departmentRepository;
    private final AuditService auditService;

    public List<DepartmentInfo> list() {
        return departmentRepository.findAll();
    }

    @Transactional
    public DepartmentInfo create(String code, String name, Long parentId, String type, int sortNo) {
        String normalizedCode = code == null ? "" : code.trim().toUpperCase();
        if (!normalizedCode.matches("[A-Z][A-Z0-9_]{1,31}")) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "科室编码需为 2-32 位大写字母、数字或下划线");
        }
        if (departmentRepository.existsByCode(normalizedCode)) {
            throw new BusinessException(ErrorCode.CONFLICT, "科室编码已存在");
        }
        validateType(type);
        validateParent(parentId, null);
        long id = departmentRepository.insert(normalizedCode, name.trim(), parentId, type, sortNo);
        auditService.success("DEPARTMENT_CREATED", "DEPARTMENT", String.valueOf(id), null,
                Map.of("code", normalizedCode, "name", name, "type", type));
        return require(id);
    }

    @Transactional
    public DepartmentInfo update(long id, String name, Long parentId, String type, int sortNo) {
        DepartmentInfo before = require(id);
        validateType(type);
        validateParent(parentId, id);
        departmentRepository.update(id, name.trim(), parentId, type, sortNo);
        auditService.success("DEPARTMENT_UPDATED", "DEPARTMENT", String.valueOf(id),
                Map.of("name", before.name(), "type", before.type(), "parentId",
                        before.parentId() == null ? 0L : before.parentId()),
                Map.of("name", name, "type", type, "parentId", parentId == null ? 0L : parentId));
        return require(id);
    }

    private void validateType(String type) {
        if (type == null || !TYPES.contains(type)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "科室类型只能是 CLINICAL/PHARMACY/ADMIN");
        }
    }

    private void validateParent(Long parentId, Long selfId) {
        if (parentId == null) {
            return;
        }
        if (parentId.equals(selfId)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "上级科室不能是自身");
        }
        if (departmentRepository.findById(parentId).isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "上级科室不存在");
        }
    }

    private DepartmentInfo require(long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "科室不存在"));
    }
}
