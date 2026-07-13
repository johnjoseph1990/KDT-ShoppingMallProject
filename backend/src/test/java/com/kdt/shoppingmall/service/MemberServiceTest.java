package com.kdt.shoppingmall.service;

import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.member.MemberRole;
import com.kdt.shoppingmall.dto.member.MemberResponse;
import com.kdt.shoppingmall.dto.member.SignupRequest;
import com.kdt.shoppingmall.exception.DuplicateEmailException;
import com.kdt.shoppingmall.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberService memberService;

    @Test
    void signup_성공() {
        SignupRequest request = new SignupRequest("test@test.com", "password123", "테스터");
        Member savedMember = new Member("test@test.com", "encoded", "테스터", MemberRole.USER);

        given(memberRepository.existsByEmail("test@test.com")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("encoded");
        given(memberRepository.save(any(Member.class))).willReturn(savedMember);

        MemberResponse response = memberService.signup(request);

        assertThat(response.email()).isEqualTo("test@test.com");
        assertThat(response.name()).isEqualTo("테스터");
        assertThat(response.role()).isEqualTo(MemberRole.USER);
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    void signup_중복이메일_예외발생() {
        SignupRequest request = new SignupRequest("dup@test.com", "password123", "중복자");
        given(memberRepository.existsByEmail("dup@test.com")).willReturn(true);

        assertThatThrownBy(() -> memberService.signup(request))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("dup@test.com");

        verify(memberRepository, never()).save(any());
    }
}
