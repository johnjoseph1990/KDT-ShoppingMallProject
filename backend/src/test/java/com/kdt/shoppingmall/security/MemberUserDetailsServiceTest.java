package com.kdt.shoppingmall.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.member.MemberRole;
import com.kdt.shoppingmall.repository.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

// Spring Security가 인증 시 호출하는 loadUserByUsername의 두 경로(성공/실패)를 검증한다.
// DB를 직접 띄울 필요 없이 MemberRepository를 Mock으로 대체하는 단위 테스트.
@ExtendWith(MockitoExtension.class)
class MemberUserDetailsServiceTest {

  @Mock private MemberRepository memberRepository;

  @InjectMocks private MemberUserDetailsService memberUserDetailsService;

  @Test
  void loadUserByUsername_이메일_존재_MemberPrincipal_반환() {
    // given
    Member member = new Member("user@test.com", "encodedPw", "테스터", MemberRole.USER);
    given(memberRepository.findByEmail("user@test.com")).willReturn(Optional.of(member));

    // when
    UserDetails result = memberUserDetailsService.loadUserByUsername("user@test.com");

    // then
    // 반환 타입이 MemberPrincipal인지, username이 이메일인지 확인
    assertThat(result).isInstanceOf(MemberPrincipal.class);
    assertThat(result.getUsername()).isEqualTo("user@test.com");
    assertThat(result.getPassword()).isEqualTo("encodedPw");
    // ROLE_USER 권한이 부여됐는지 확인
    assertThat(result.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
  }

  @Test
  void loadUserByUsername_ADMIN_권한_정상_매핑() {
    // ROLE_ADMIN도 올바르게 매핑되는지 확인
    Member admin = new Member("admin@test.com", "encodedPw", "관리자", MemberRole.ADMIN);
    given(memberRepository.findByEmail("admin@test.com")).willReturn(Optional.of(admin));

    UserDetails result = memberUserDetailsService.loadUserByUsername("admin@test.com");

    assertThat(result.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
  }

  @Test
  void loadUserByUsername_이메일_없으면_UsernameNotFoundException_발생() {
    // Spring Security 인증 실패 경로: 없는 이메일로 조회하면 UsernameNotFoundException을 던진다
    given(memberRepository.findByEmail("unknown@test.com")).willReturn(Optional.empty());

    assertThatThrownBy(() -> memberUserDetailsService.loadUserByUsername("unknown@test.com"))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessageContaining("unknown@test.com");
  }
}
