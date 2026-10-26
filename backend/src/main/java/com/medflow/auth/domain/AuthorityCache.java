package com.medflow.auth.domain;

import java.util.Optional;

public interface AuthorityCache {

    Optional<AuthoritySnapshot> get(long userId);

    void put(long userId, AuthoritySnapshot snapshot);

    void evict(long userId);
}
