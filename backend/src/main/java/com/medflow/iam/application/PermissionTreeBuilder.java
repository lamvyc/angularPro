package com.medflow.iam.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.medflow.iam.domain.PermissionInfo;
import com.medflow.iam.domain.PermissionNode;
import org.springframework.stereotype.Component;

@Component
public class PermissionTreeBuilder {

    public List<PermissionNode> build(List<PermissionInfo> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return List.of();
        }
        Map<Long, PermissionInfo> byId = new LinkedHashMap<>();
        permissions.forEach(permission -> byId.put(permission.id(), permission));
        Map<Long, List<PermissionInfo>> children = new LinkedHashMap<>();
        List<PermissionInfo> roots = new ArrayList<>();
        for (PermissionInfo permission : permissions) {
            if (permission.parentId() != null && byId.containsKey(permission.parentId())) {
                children.computeIfAbsent(permission.parentId(), key -> new ArrayList<>()).add(permission);
            } else {
                roots.add(permission);
            }
        }
        roots.sort(Comparator.comparingInt(PermissionInfo::sortNo));
        return roots.stream().map(root -> toNode(root, children)).toList();
    }

    private PermissionNode toNode(PermissionInfo permission, Map<Long, List<PermissionInfo>> children) {
        List<PermissionInfo> childPermissions = new ArrayList<>(
                children.getOrDefault(permission.id(), List.of()));
        childPermissions.sort(Comparator.comparingInt(PermissionInfo::sortNo));
        List<PermissionNode> nodes = childPermissions.stream()
                .map(child -> toNode(child, children))
                .toList();
        return new PermissionNode(permission.id(), permission.code(), permission.name(),
                permission.type(), permission.parentId(), nodes);
    }
}
