package com.medflow.iam.application;

import java.util.List;

import com.medflow.iam.domain.PermissionNode;
import com.medflow.iam.domain.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PermissionAppService {

    private final PermissionRepository permissionRepository;
    private final PermissionTreeBuilder treeBuilder;

    public List<PermissionNode> tree() {
        return treeBuilder.build(permissionRepository.findAll());
    }
}
