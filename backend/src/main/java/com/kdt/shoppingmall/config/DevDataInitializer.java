package com.kdt.shoppingmall.config;

import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.member.MemberRole;
import com.kdt.shoppingmall.repository.MemberRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DevDataInitializer {

    @Bean
    public ApplicationRunner seedAdmin(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        return (ApplicationArguments args) -> {
            if (!memberRepository.existsByEmail("admin@shop.com")) {
                memberRepository.save(new Member(
                        "admin@shop.com", passwordEncoder.encode("admin1234"), "관리자", MemberRole.ADMIN));
            }
        };
    }
}
