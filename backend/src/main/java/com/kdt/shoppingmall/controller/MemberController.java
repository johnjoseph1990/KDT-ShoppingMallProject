package com.kdt.shoppingmall.controller;

import com.kdt.shoppingmall.dto.member.MemberResponse;
import com.kdt.shoppingmall.dto.member.MemberUpdateRequest;
import com.kdt.shoppingmall.security.MemberPrincipal;
import com.kdt.shoppingmall.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 컨트롤러는 HTTP 요청을 받는 첫 번째 문지기 역할이다.
// 자바 관점에서 보면 '클래스(설계도)' 안의 메서드가 실제 행동을 담당하고,
// 스프링에서는 이 메서드가 요청을 처리하는 '핸들러'가 된다.
// 로그인한 회원 자신의 정보를 수정·삭제하는 컨트롤러.
// 인증(로그인 여부)은 SecurityConfig의 anyRequest().authenticated()가 처리하므로
// 여기서는 @AuthenticationPrincipal로 현재 로그인 회원 ID만 꺼내서 쓴다.
@RestController
@RequestMapping("/api/members")
public class MemberController {

  private final MemberService memberService;

  public MemberController(MemberService memberService) {
    this.memberService = memberService;
  }

  // PUT /api/members/me — 이름·비밀번호 수정 (변경할 항목만 요청 바디에 포함)
  // 스프링은 @PutMapping으로 이 메서드가 PUT 요청을 처리하는지 알려준다.
  // 여기서는 컨트롤러가 요청을 받아 서비스로 넘기고, 결과를 다시 응답으로 돌려준다.
  @PutMapping("/me")
  public MemberResponse update(
      @AuthenticationPrincipal MemberPrincipal principal,
      @Valid @RequestBody MemberUpdateRequest request) {
    return memberService.update(principal.getMember().getId(), request);
  }

  // DELETE /api/members/me — 회원 탈퇴. DB에서 삭제 후 세션도 즉시 무효화한다.
  // 스프링은 @DeleteMapping으로 이 메서드가 DELETE 요청을 처리하는지 알려준다.
  // 절차적으로 보면 '요청 받기 → 서비스 호출 → 세션 정리 → 응답' 순서로 진행된다.
  @DeleteMapping("/me")
  public ResponseEntity<Void> delete(
      @AuthenticationPrincipal MemberPrincipal principal, HttpServletRequest httpRequest) {
    memberService.delete(principal.getMember().getId());
    // 탈퇴 처리 후 세션을 끊어 이후 요청이 인증된 것처럼 처리되지 않게 막는다
    httpRequest.getSession().invalidate();
    SecurityContextHolder.clearContext();
    return ResponseEntity.noContent().build();
  }
}
