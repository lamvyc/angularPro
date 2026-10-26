package com.medflow.common.security;

import java.util.Optional;

/**
 * 由 auth 模块实现：校验 JWT、拒绝名单、加载权限快照。
 */
public interface JwtAuthenticator {

    Optional<CurrentUser> authenticate(String rawToken);
}
