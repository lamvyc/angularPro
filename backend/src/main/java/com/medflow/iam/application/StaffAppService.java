package com.medflow.iam.application;

import java.util.List;

import com.medflow.common.security.DataScopeResolver;
import com.medflow.iam.domain.DoctorView;
import com.medflow.iam.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StaffAppService {

    private final UserRepository userRepository;
    private final DataScopeResolver dataScopeResolver;

    public List<DoctorView> doctors(Long requestedDeptId) {
        Long deptFilter = dataScopeResolver.resolveDepartmentFilter(requestedDeptId).orElse(null);
        return userRepository.findDoctors(deptFilter).stream()
                .map(user -> new DoctorView(user.id(), user.realName(), user.deptId(),
                        user.deptName(), user.title()))
                .toList();
    }
}
