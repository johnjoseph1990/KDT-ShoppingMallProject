package com.kdt.shoppingmall.support;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockMemberPrincipalSecurityContextFactory.class)
public @interface WithMockMemberPrincipal {
    long id() default 1L;
    String email() default "test@test.com";
    String name() default "테스터";
    String role() default "USER";
}
