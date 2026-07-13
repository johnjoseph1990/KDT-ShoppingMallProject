package com.kdt.shoppingmall.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.member.MemberRole;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class MemberRepositoryTest {

  @Autowired private TestEntityManager em;

  @Autowired private MemberRepository memberRepository;

  @Test
  void findByEmail_존재하는이메일() {
    em.persistAndFlush(new Member("test@test.com", "encoded", "테스터", MemberRole.USER));

    Optional<Member> result = memberRepository.findByEmail("test@test.com");

    assertThat(result).isPresent();
    assertThat(result.get().getEmail()).isEqualTo("test@test.com");
  }

  @Test
  void findByEmail_존재하지않는이메일() {
    Optional<Member> result = memberRepository.findByEmail("none@test.com");

    assertThat(result).isEmpty();
  }

  @Test
  void existsByEmail_true() {
    em.persistAndFlush(new Member("exist@test.com", "encoded", "존재", MemberRole.USER));

    assertThat(memberRepository.existsByEmail("exist@test.com")).isTrue();
  }

  @Test
  void existsByEmail_false() {
    assertThat(memberRepository.existsByEmail("none@test.com")).isFalse();
  }
}
