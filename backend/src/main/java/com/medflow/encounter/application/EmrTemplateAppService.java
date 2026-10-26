package com.medflow.encounter.application;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.medflow.common.error.BusinessException;
import com.medflow.common.error.ErrorCode;
import com.medflow.common.security.CurrentUser;
import com.medflow.common.security.SecurityUtils;
import com.medflow.encounter.domain.EmrTemplateRow;
import com.medflow.encounter.domain.EncounterRepository;
import com.medflow.iam.application.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class EmrTemplateAppService {

    private final EncounterRepository repository;
    private final AuditService auditService;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    public List<EmrTemplateRow> list() {
        return repository.listCurrentTemplates();
    }

    public List<EmrTemplateRow> versions(long templateId) {
        require(templateId);
        return repository.listTemplateVersions(templateId);
    }

    @Transactional
    public EmrTemplateRow create(String code, String name, String scopeType, Long deptId,
            Map<String, Object> schema, String changelog) {
        validateSchema(schema);
        String normalizedCode = code == null ? "" : code.trim().toUpperCase();
        if (normalizedCode.isBlank() || name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "模板编码与名称不能为空");
        }
        if (!"HOSPITAL".equals(scopeType) && !"DEPT".equals(scopeType)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "模板范围只能是 HOSPITAL 或 DEPT");
        }
        if ("DEPT".equals(scopeType) && deptId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "科室模板必须指定科室");
        }
        CurrentUser user = SecurityUtils.requireCurrentUser();
        Instant now = Instant.now(clock);
        long templateId;
        try {
            templateId = repository.insertTemplate(normalizedCode, name.trim(), scopeType, deptId, user.id());
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.CONFLICT, "模板编码已存在");
        }
        long versionId = repository.insertTemplateVersion(templateId, 1, jsonMapper.writeValueAsString(schema),
                changelog, user.id(), now);
        repository.setTemplateCurrentVersion(templateId, versionId, user.id());
        auditService.success("EMR_TEMPLATE_CREATED", "EMR_TEMPLATE", String.valueOf(templateId), null,
                mapOf("code", normalizedCode, "version", 1));
        return require(templateId);
    }

    @Transactional
    public EmrTemplateRow publish(long templateId, Map<String, Object> schema, String changelog) {
        validateSchema(schema);
        EmrTemplateRow template = require(templateId);
        if ("ARCHIVED".equals(template.status())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "已归档模板不能发布新版本");
        }
        CurrentUser user = SecurityUtils.requireCurrentUser();
        int nextVersion = template.versionNo() + 1;
        long versionId = repository.insertTemplateVersion(templateId, nextVersion,
                jsonMapper.writeValueAsString(schema), changelog, user.id(), Instant.now(clock));
        repository.setTemplateCurrentVersion(templateId, versionId, user.id());
        auditService.success("EMR_TEMPLATE_PUBLISHED", "EMR_TEMPLATE", String.valueOf(templateId),
                mapOf("version", template.versionNo()), mapOf("version", nextVersion));
        return require(templateId);
    }

    @Transactional
    public EmrTemplateRow archive(long templateId) {
        EmrTemplateRow template = require(templateId);
        CurrentUser user = SecurityUtils.requireCurrentUser();
        repository.archiveTemplate(templateId, user.id());
        auditService.success("EMR_TEMPLATE_ARCHIVED", "EMR_TEMPLATE", String.valueOf(templateId),
                mapOf("status", template.status()), mapOf("status", "ARCHIVED"));
        return require(templateId);
    }

    private EmrTemplateRow require(long id) {
        return repository.findTemplate(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "模板不存在"));
    }

    @SuppressWarnings("unchecked")
    private void validateSchema(Map<String, Object> schema) {
        if (schema == null || !(schema.get("sections") instanceof List<?> sections) || sections.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "模板必须包含至少一个分区");
        }
        for (Object section : sections) {
            if (!(section instanceof Map<?, ?> sectionMap)
                    || !(sectionMap.get("fields") instanceof List<?> fields) || fields.isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "每个分区必须包含字段");
            }
            for (Object field : fields) {
                if (!(field instanceof Map<?, ?> fieldMap) || fieldMap.get("key") == null
                        || fieldMap.get("type") == null) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED, "字段必须包含 key 与 type");
                }
            }
        }
    }

    private Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }
}
