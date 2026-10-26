package com.medflow.prescription.api;

import java.util.List;

import com.medflow.prescription.application.PrescriptionAppService;
import com.medflow.prescription.domain.PrescriptionModels.Drug;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/drugs")
@Tag(name = "Drug", description = "药品目录")
@RequiredArgsConstructor
public class DrugController {

    private final PrescriptionAppService prescriptionAppService;

    @GetMapping
    @PreAuthorize("hasAuthority('drug:read')")
    public List<Drug> list() {
        return prescriptionAppService.drugs();
    }
}
