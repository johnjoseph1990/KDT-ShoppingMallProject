package com.kdt.shoppingmall.support;

import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.member.MemberRole;
import com.kdt.shoppingmall.security.MemberPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.test.util.ReflectionTestUtils;

public class WithMockMemberPrincipalSecurityContextFactory
        implements WithSecurityContextFactory<WithMockMemberPrincipal> {

    @Override
    public SecurityContext createSecurityContext(WithMockMemberPrincipal annotation) {
        Member member = new Member(annotation.email(), "encoded", annotation.name(),
                MemberRole.valueOf(annotation.role()));
        ReflectionTestUtils.setField(member, "id", annotation.id());

        MemberPrincipal principal = new MemberPrincipal(member);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        return context;
    }
}
