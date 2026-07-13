package com.kdt.shoppingmall.support;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.springframework.security.test.context.support.WithSecurityContext;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockMemberPrincipalSecurityContextFactory.class)
public @interface WithMockMemberPrincipal {
  long id() default 1L;

  String email() default "test@test.com";

  String name() default "테스터";

  String role() default "USER";
}
