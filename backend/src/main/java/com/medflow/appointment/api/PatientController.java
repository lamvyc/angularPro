package com.medflow.appointment.api;

import com.medflow.appointment.api.dto.CreatePatientRequest;
import com.medflow.appointment.application.CreatePatientCommand;
import com.medflow.appointment.application.PatientAppService;
import com.medflow.appointment.domain.Patient;
import com.medflow.common.web.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patients")
@Tag(name = "Patient", description = "患者建档与检索")
@RequiredArgsConstructor
public class PatientController {

    private final PatientAppService patientAppService;

    @GetMapping
    @PreAuthorize("hasAuthority('patient:read')")
    @Operation(summary = "分页检索患者")
    public PageResult<Patient> search(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        return patientAppService.search(page, size, keyword);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('patient:read')")
    public Patient get(@PathVariable long id) {
        return patientAppService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('patient:create')")
    @Operation(summary = "患者建档")
    public Patient create(@Valid @RequestBody CreatePatientRequest request) {
        return patientAppService.create(new CreatePatientCommand(
                request.name(), request.gender(), request.birthDate(), request.idCardNo(),
                request.phone(), request.address(), request.allergyHistory()));
    }
}
