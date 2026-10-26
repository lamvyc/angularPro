package com.medflow.iam.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.medflow.iam.domain.MenuNode;
import com.medflow.iam.domain.PermissionInfo;
import org.springframework.stereotype.Component;

@Component
public class MenuTreeBuilder {

    public List<MenuNode> build(List<PermissionInfo> menus) {
        if (menus == null || menus.isEmpty()) {
            return List.of();
        }
        Map<Long, PermissionInfo> byId = new LinkedHashMap<>();
        menus.forEach(menu -> byId.put(menu.id(), menu));

        Map<Long, List<PermissionInfo>> children = new LinkedHashMap<>();
        List<PermissionInfo> roots = new ArrayList<>();
        for (PermissionInfo menu : menus) {
            if (menu.parentId() != null && byId.containsKey(menu.parentId())) {
                children.computeIfAbsent(menu.parentId(), key -> new ArrayList<>()).add(menu);
            } else {
                roots.add(menu);
            }
        }
        roots.sort(Comparator.comparingInt(PermissionInfo::sortNo));
        return roots.stream().map(root -> toNode(root, children)).toList();
    }

    private MenuNode toNode(PermissionInfo menu, Map<Long, List<PermissionInfo>> children) {
        List<PermissionInfo> childMenus = new ArrayList<>(children.getOrDefault(menu.id(), List.of()));
        childMenus.sort(Comparator.comparingInt(PermissionInfo::sortNo));
        List<MenuNode> nodes = childMenus.stream().map(child -> toNode(child, children)).toList();
        return new MenuNode(menu.id(), menu.code(), menu.name(), menu.routePath(), menu.icon(), nodes);
    }
}
