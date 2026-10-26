package com.medflow.encounter.api;

import java.util.List;

import com.medflow.encounter.api.dto.CreateEmrTemplateRequest;
import com.medflow.encounter.api.dto.PublishEmrTemplateRequest;
import com.medflow.encounter.application.EmrTemplateAppService;
import com.medflow.encounter.domain.EmrTemplateRow;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/emr-templates")
@Tag(name = "EMR Template", description = "结构化病历模板与版本")
@RequiredArgsConstructor
public class EmrTemplateController {

    private final EmrTemplateAppService templateAppService;

    @GetMapping
    @PreAuthorize("hasAuthority('emr:template:read')")
    public List<EmrTemplateRow> list() {
        return templateAppService.list();
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("hasAuthority('emr:template:read')")
    public List<EmrTemplateRow> versions(@PathVariable long id) {
        return templateAppService.versions(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('emr:template:manage')")
    public EmrTemplateRow create(@Valid @RequestBody CreateEmrTemplateRequest request) {
        return templateAppService.create(request.code(), request.name(), request.scopeType(),
                request.deptId(), request.schema(), request.changelog());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('emr:template:manage')")
    public EmrTemplateRow publish(@PathVariable long id,
            @Valid @RequestBody PublishEmrTemplateRequest request) {
        return templateAppService.publish(id, request.schema(), request.changelog());
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('emr:template:manage')")
    public EmrTemplateRow archive(@PathVariable long id) {
        return templateAppService.archive(id);
    }
}
