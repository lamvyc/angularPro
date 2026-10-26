package com.medflow.common.security;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * 数据范围：取用户所有角色中最大的范围。
 */
public enum DataScope {
    SELF(1),
    DEPT(2),
    ALL(3);

    private final int rank;

    DataScope(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }

    public static DataScope strongest(Collection<DataScope> scopes) {
        return scopes.stream().max(Comparator.comparingInt(DataScope::rank)).orElse(DataScope.SELF);
    }

    public static List<String> names() {
        return List.of(ALL.name(), DEPT.name(), SELF.name());
    }
}
