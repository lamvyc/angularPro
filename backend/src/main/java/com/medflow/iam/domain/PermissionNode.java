package com.medflow.iam.domain;

import java.util.List;

public record PermissionNode(
        long id,
        String code,
        String name,
        String type,
        Long parentId,
        List<PermissionNode> children) {

    public PermissionNode {
        children = children == null ? List.of() : List.copyOf(children);
    }
}
