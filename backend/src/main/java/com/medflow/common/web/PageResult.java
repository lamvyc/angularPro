package com.medflow.common.web;

import java.util.List;

public record PageResult<T>(List<T> items, long page, long size, long total) {

    public PageResult {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static <T> PageResult<T> of(List<T> items, long page, long size, long total) {
        return new PageResult<>(items, page, size, total);
    }
}
