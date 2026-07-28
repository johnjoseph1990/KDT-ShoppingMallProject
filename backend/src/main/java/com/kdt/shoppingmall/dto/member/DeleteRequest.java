package com.kdt.shoppingmall.dto.member;

import jakarta.validation.constraints.NotBlank;

// 회원 탈퇴 요청 DTO. 세션 탈취 시 공격자가 계정을 삭제하는 것을 막기 위해 비밀번호 재확인이 필요하다.
public record DeleteRequest(@NotBlank(message = "비밀번호를 입력해주세요.") String password) {}
