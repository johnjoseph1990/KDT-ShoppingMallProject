package com.kdt.shoppingmall.dto.member;

import jakarta.validation.constraints.Size;

// 회원 정보 수정 요청 DTO.
// name만 변경, 비밀번호만 변경, 둘 다 변경 모두 허용한다.
// 비밀번호를 변경할 때는 currentPassword가 반드시 필요하다.
public record MemberUpdateRequest(
    String name, // null이면 이름 변경 안 함
    String currentPassword, // 비밀번호 변경 시 현재 비밀번호 확인용
    @Size(min = 8, message = "새 비밀번호는 8자 이상이어야 합니다.") String newPassword // null이면 비밀번호 변경 안 함
    ) {}
