package com.medflow.pharmacy.api;

import java.util.List;

import com.medflow.pharmacy.api.dto.DrugRequest;
import com.medflow.pharmacy.api.dto.DrugStatusRequest;
import com.medflow.pharmacy.application.PharmacyAppService;
import com.medflow.pharmacy.domain.PharmacyModels.DrugAdmin;
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
@RequestMapping("/api/v1/drugs")
@Tag(name = "Drug Admin", description = "药品目录维护")
@RequiredArgsConstructor
public class DrugAdminController {

    private final PharmacyAppService pharmacyAppService;

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('drug:manage')")
    public List<DrugAdmin> list() {
        return pharmacyAppService.drugs();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('drug:manage')")
    public DrugAdmin create(@Valid @RequestBody DrugRequest request) {
        return pharmacyAppService.createDrug(request.code(), request.name(), request.genericName(),
                request.spec(), request.unit(), request.dosageForm(), request.manufacturer(),
                request.category(), request.unitPrice(), request.prescriptionType());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('drug:manage')")
    public DrugAdmin update(@PathVariable long id, @Valid @RequestBody DrugRequest request) {
        return pharmacyAppService.updateDrug(id, request.name(), request.genericName(), request.spec(),
                request.unit(), request.dosageForm(), request.manufacturer(), request.category(),
                request.unitPrice(), request.prescriptionType());
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('drug:manage')")
    public DrugAdmin status(@PathVariable long id, @Valid @RequestBody DrugStatusRequest request) {
        return pharmacyAppService.setDrugStatus(id, request.status());
    }
}
