package com.kdt.shoppingmall.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.member.MemberRole;
import com.kdt.shoppingmall.dto.member.MemberResponse;
import com.kdt.shoppingmall.dto.member.MemberUpdateRequest;
import com.kdt.shoppingmall.dto.member.SignupRequest;
import com.kdt.shoppingmall.exception.DuplicateEmailException;
import com.kdt.shoppingmall.exception.PasswordMismatchException;
import com.kdt.shoppingmall.exception.ResourceNotFoundException;
import com.kdt.shoppingmall.repository.AddressRepository;
import com.kdt.shoppingmall.repository.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

  @Mock private MemberRepository memberRepository;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private AddressRepository addressRepository;

  @InjectMocks private MemberService memberService;

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

  @Test
  void update_이름변경_성공() {
    Member member = new Member("test@test.com", "encoded", "기존이름", MemberRole.USER);
    ReflectionTestUtils.setField(member, "id", 1L);
    given(memberRepository.findById(1L)).willReturn(Optional.of(member));

    MemberUpdateRequest request = new MemberUpdateRequest("새이름", null, null);
    MemberResponse response = memberService.update(1L, request);

    assertThat(response.name()).isEqualTo("새이름");
  }

  @Test
  void update_비밀번호변경_성공() {
    Member member = new Member("test@test.com", "encoded_old", "테스터", MemberRole.USER);
    ReflectionTestUtils.setField(member, "id", 1L);
    given(memberRepository.findById(1L)).willReturn(Optional.of(member));
    // 현재 비밀번호 일치 확인
    given(passwordEncoder.matches("oldPass", "encoded_old")).willReturn(true);
    given(passwordEncoder.encode("newPass123")).willReturn("encoded_new");

    MemberUpdateRequest request = new MemberUpdateRequest(null, "oldPass", "newPass123");
    memberService.update(1L, request);

    // member.password가 바뀌었는지 검증
    assertThat(member.getPassword()).isEqualTo("encoded_new");
  }

  @Test
  void update_현재비밀번호_틀림_예외발생() {
    Member member = new Member("test@test.com", "encoded_old", "테스터", MemberRole.USER);
    ReflectionTestUtils.setField(member, "id", 1L);
    given(memberRepository.findById(1L)).willReturn(Optional.of(member));
    given(passwordEncoder.matches("wrongPass", "encoded_old")).willReturn(false);

    MemberUpdateRequest request = new MemberUpdateRequest(null, "wrongPass", "newPass123");

    assertThatThrownBy(() -> memberService.update(1L, request))
        .isInstanceOf(PasswordMismatchException.class);
  }

  @Test
  void delete_성공() {
    Member member = new Member("test@test.com", "encoded", "테스터", MemberRole.USER);
    ReflectionTestUtils.setField(member, "id", 1L);
    given(memberRepository.findById(1L)).willReturn(Optional.of(member));

    memberService.delete(1L);

    // 배송지가 회원보다 먼저 삭제되어야 FK 제약 위반이 발생하지 않는다
    verify(addressRepository).deleteAllByMemberId(1L);
    verify(memberRepository).delete(member);
  }

  @Test
  void update_이름과_비밀번호_동시변경_성공() {
    // 이름과 비밀번호를 한 번의 요청으로 함께 바꿀 수 있어야 한다
    Member member = new Member("test@test.com", "encoded_old", "기존이름", MemberRole.USER);
    ReflectionTestUtils.setField(member, "id", 1L);
    given(memberRepository.findById(1L)).willReturn(Optional.of(member));
    given(passwordEncoder.matches("oldPass", "encoded_old")).willReturn(true);
    given(passwordEncoder.encode("newPass123")).willReturn("encoded_new");

    MemberUpdateRequest request = new MemberUpdateRequest("새이름", "oldPass", "newPass123");
    MemberResponse response = memberService.update(1L, request);

    assertThat(response.name()).isEqualTo("새이름");
    assertThat(member.getPassword()).isEqualTo("encoded_new");
  }

  @Test
  void update_존재하지않는_회원_예외발생() {
    // 없는 ID로 수정 요청 시 ResourceNotFoundException이 발생해야 한다
    given(memberRepository.findById(99L)).willReturn(Optional.empty());

    MemberUpdateRequest request = new MemberUpdateRequest("새이름", null, null);

    assertThatThrownBy(() -> memberService.update(99L, request))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void update_currentPassword_null인데_newPassword_요청시_예외발생() {
    // currentPassword 없이 newPassword만 보내면 현재 비밀번호 검증이 실패해야 한다
    Member member = new Member("test@test.com", "encoded_old", "테스터", MemberRole.USER);
    ReflectionTestUtils.setField(member, "id", 1L);
    given(memberRepository.findById(1L)).willReturn(Optional.of(member));
    // currentPassword가 null이면 matches()가 호출되지 않으므로 stub 불필요

    MemberUpdateRequest request = new MemberUpdateRequest(null, null, "newPass123");

    assertThatThrownBy(() -> memberService.update(1L, request))
        .isInstanceOf(PasswordMismatchException.class);
  }

  @Test
  void delete_존재하지않는_회원_예외발생() {
    // 없는 ID로 탈퇴 요청 시 ResourceNotFoundException이 발생해야 한다
    given(memberRepository.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> memberService.delete(99L))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(memberRepository, never()).delete(any());
  }
}
