package com.kdt.shoppingmall.service;

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
import com.kdt.shoppingmall.repository.ReviewKeywordRepository;
import com.kdt.shoppingmall.repository.ReviewRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 서비스는 컨트롤러와 저장소 사이에서 비즈니스 로직을 처리하는 중간 계층이다.
// 자바에서는 메서드가 '기능'을 담고, 스프링에서는 이 클래스가 그 기능을 모아둔 '서비스'로 동작한다.
@Service
@Transactional(readOnly = true)
public class MemberService {

  private final MemberRepository memberRepository;
  private final PasswordEncoder passwordEncoder;
  private final AddressRepository addressRepository;
  private final ReviewKeywordRepository reviewKeywordRepository;
  private final ReviewRepository reviewRepository;

  public MemberService(
      MemberRepository memberRepository,
      PasswordEncoder passwordEncoder,
      AddressRepository addressRepository,
      ReviewKeywordRepository reviewKeywordRepository,
      ReviewRepository reviewRepository) {
    this.memberRepository = memberRepository;
    this.passwordEncoder = passwordEncoder;
    this.addressRepository = addressRepository;
    this.reviewKeywordRepository = reviewKeywordRepository;
    this.reviewRepository = reviewRepository;
  }

  // 회원 가입은 '입력 받은 정보를 검증하고, 비밀번호를 암호화한 뒤, DB에 저장'하는 절차다.
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

  // 회원 정보 수정은 '기존 회원을 찾고, 비밀번호 변경 요청이 있으면 현재 비밀번호를 확인한 뒤,
  // 새 값으로 바꾸는' 순서로 진행된다. 스프링에서 메서드 하나가 하나의 절차를 의미한다.
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

  // 회원 탈퇴: FK 제약 위반 방지를 위해 자식 테이블 순서대로 삭제한다.
  // 순서: review_keyword → review → address → member
  // review_keyword는 review를 참조하고, review는 member를 참조하므로
  // 부모(member)를 지우기 전에 자식을 역순으로 지워야 DB 제약이 깨지지 않는다.
  // rawPassword: 세션 탈취 시 공격자가 계정을 삭제하는 것을 막기 위해 비밀번호를 재확인한다.
  @Transactional
  public void delete(Long memberId, String rawPassword) {
    Member member =
        memberRepository
            .findById(memberId)
            .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 회원입니다."));
    if (!passwordEncoder.matches(rawPassword, member.getPassword())) {
      throw new PasswordMismatchException("현재 비밀번호가 올바르지 않습니다.");
    }
    reviewKeywordRepository.deleteByReviewMemberId(memberId);
    reviewRepository.deleteAllByMemberId(memberId);
    addressRepository.deleteAllByMemberId(memberId);
    memberRepository.delete(member);
  }
}
