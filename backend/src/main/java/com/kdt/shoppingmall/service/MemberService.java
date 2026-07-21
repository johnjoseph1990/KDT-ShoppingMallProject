package com.kdt.shoppingmall.service;

import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.member.MemberRole;
import com.kdt.shoppingmall.dto.member.MemberResponse;
import com.kdt.shoppingmall.dto.member.MemberUpdateRequest;
import com.kdt.shoppingmall.dto.member.SignupRequest;
import com.kdt.shoppingmall.exception.DuplicateEmailException;
import com.kdt.shoppingmall.exception.PasswordMismatchException;
import com.kdt.shoppingmall.exception.ResourceNotFoundException;
import com.kdt.shoppingmall.repository.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MemberService {

  private final MemberRepository memberRepository;
  private final PasswordEncoder passwordEncoder;

  public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
    this.memberRepository = memberRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public MemberResponse signup(SignupRequest request) {
    if (memberRepository.existsByEmail(request.email())) {
      throw new DuplicateEmailException("이미 가입된 이메일입니다: " + request.email());
    }
    Member member =
        new Member(
            request.email(),
            passwordEncoder.encode(request.password()),
            request.name(),
            MemberRole.USER);
    return MemberResponse.from(memberRepository.save(member));
  }

  // 이름·비밀번호를 선택적으로 수정한다. 비밀번호 변경 시 현재 비밀번호를 먼저 검증한다.
  @Transactional
  public MemberResponse update(Long memberId, MemberUpdateRequest request) {
    Member member =
        memberRepository
            .findById(memberId)
            .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 회원입니다."));

    // 새 비밀번호가 있으면 현재 비밀번호가 맞는지 확인
    String encodedNewPassword = null;
    if (request.newPassword() != null) {
      if (request.currentPassword() == null
          || !passwordEncoder.matches(request.currentPassword(), member.getPassword())) {
        throw new PasswordMismatchException("현재 비밀번호가 올바르지 않습니다.");
      }
      encodedNewPassword = passwordEncoder.encode(request.newPassword());
    }

    member.update(request.name(), encodedNewPassword);
    return MemberResponse.from(member);
  }

  // 회원을 삭제한다. JPA가 DELETE SQL을 실행하며, 호출자가 세션을 무효화해야 한다.
  @Transactional
  public void delete(Long memberId) {
    Member member =
        memberRepository
            .findById(memberId)
            .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 회원입니다."));
    memberRepository.delete(member);
  }
}
