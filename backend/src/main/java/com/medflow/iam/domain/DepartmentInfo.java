package com.medflow.iam.domain;

public record DepartmentInfo(long id, String code, String name, Long parentId, String type, String status,
        int sortNo) {
}
