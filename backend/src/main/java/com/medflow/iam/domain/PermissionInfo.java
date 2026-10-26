package com.medflow.iam.domain;

public record PermissionInfo(
        long id,
        String code,
        String name,
        String type,
        Long parentId,
        String routePath,
        String icon,
        int sortNo) {
}
