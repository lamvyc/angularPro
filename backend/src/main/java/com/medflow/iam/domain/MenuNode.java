package com.medflow.iam.domain;

import java.util.List;

public record MenuNode(long id, String code, String name, String path, String icon, List<MenuNode> children) {

    public MenuNode {
        children = children == null ? List.of() : List.copyOf(children);
    }
}
