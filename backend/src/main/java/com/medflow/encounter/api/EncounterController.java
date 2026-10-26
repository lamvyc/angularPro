package com.medflow.encounter.api;

import java.time.LocalDate;
import java.util.List;

import com.medflow.encounter.api.dto.CancelEncounterRequest;
import com.medflow.encounter.api.dto.EmrAmendRequest;
import com.medflow.encounter.api.dto.DiagnosesRequest;
import com.medflow.encounter.api.dto.EmrSaveRequest;
import com.medflow.encounter.application.EncounterAppService;
import com.medflow.encounter.domain.DiagnosisView;
import com.medflow.encounter.domain.EmrView;
import com.medflow.encounter.domain.EmrVersionView;
import com.medflow.encounter.domain.EncounterDetail;
import com.medflow.encounter.domain.EncounterRow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/encounters")
@Tag(name = "Encounter", description = "医生工作站：候诊、接诊、病历草稿与诊断")
@RequiredArgsConstructor
public class EncounterController {

    private final EncounterAppService encounterAppService;

    @GetMapping("/queue")
    @PreAuthorize("hasAuthority('encounter:read')")
    @Operation(summary = "候诊队列（医生本人 / 护士本科室）")
    public List<EncounterRow> queue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return encounterAppService.queue(date);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('encounter:read')")
    public EncounterDetail detail(@PathVariable long id) {
        return encounterAppService.detail(id);
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAuthority('encounter:start')")
    public EncounterDetail start(@PathVariable long id) {
        return encounterAppService.start(id);
    }

    @PutMapping("/{id}/emr")
    @PreAuthorize("hasAuthority('emr:edit')")
    @Operation(summary = "保存病历草稿（乐观锁）")
    public EmrView saveEmr(@PathVariable long id, @Valid @RequestBody EmrSaveRequest request) {
        return encounterAppService.saveEmr(id, request.content(), request.version());
    }

    @PostMapping("/{id}/emr/sign")
    @PreAuthorize("hasAuthority('emr:sign')")
    @Operation(summary = "签署病历（模板校验 + 内容哈希）")
    public EmrView sign(@PathVariable long id, @Valid @RequestBody EmrSaveRequest request) {
        return encounterAppService.sign(id, request.content(), request.version());
    }

    @PostMapping("/{id}/emr/amend")
    @PreAuthorize("hasAuthority('emr:revise')")
    @Operation(summary = "修订已签署病历，生成新版本")
    public EmrView amend(@PathVariable long id, @Valid @RequestBody EmrAmendRequest request) {
        return encounterAppService.amend(id, request.content(), request.version(), request.reason());
    }

    @GetMapping("/{id}/emr/versions")
    @PreAuthorize("hasAuthority('emr:read')")
    public List<EmrVersionView> versions(@PathVariable long id) {
        return encounterAppService.versions(id);
    }

    @GetMapping("/{id}/emr/versions/{versionNo}")
    @PreAuthorize("hasAuthority('emr:read')")
    public EmrVersionView version(@PathVariable long id, @PathVariable int versionNo) {
        return encounterAppService.version(id, versionNo);
    }

    @PutMapping("/{id}/diagnoses")
    @PreAuthorize("hasAuthority('emr:edit')")
    @Operation(summary = "覆盖式保存诊断（必须一条主诊断）")
    public List<DiagnosisView> saveDiagnoses(@PathVariable long id,
            @Valid @RequestBody DiagnosesRequest request) {
        return encounterAppService.saveDiagnoses(id, request.diagnoses().stream()
                .map(item -> new EncounterAppService.DiagnosisCommand(
                        item.code(), item.name(), item.type(), item.note()))
                .toList());
    }

    @PostMapping("/{id}/finish")
    @PreAuthorize("hasAuthority('encounter:finish')")
    @Operation(summary = "结束就诊（校验主诉、主诊断）")
    public EncounterDetail finish(@PathVariable long id) {
        return encounterAppService.finish(id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('encounter:finish')")
    @Operation(summary = "取消就诊")
    public EncounterDetail cancel(@PathVariable long id,
            @RequestBody(required = false) CancelEncounterRequest request) {
        return encounterAppService.cancel(id, request == null ? null : request.reason());
    }
}
