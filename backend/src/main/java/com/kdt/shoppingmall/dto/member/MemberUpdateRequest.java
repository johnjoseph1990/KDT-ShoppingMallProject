package com.kdt.shoppingmall.dto.member;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 회원 정보 수정 요청 DTO.
// name만 변경, 비밀번호만 변경, 둘 다 변경 모두 허용한다.
// 비밀번호를 변경할 때는 currentPassword가 반드시 필요하다.
public record MemberUpdateRequest(
    // null = 변경 안 함. 값을 보냈다면 빈 문자열은 허용하지 않는다.
    // @Size는 null을 유효로 취급하므로 "이름 변경 안 함(null)"과 "빈 이름 금지"를 동시에 표현할 수 있다.
    @Size(min = 1, message = "이름은 1자 이상이어야 합니다.") String name,
    String currentPassword, // 비밀번호 변경 시 현재 비밀번호 확인용
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
            message = "새 비밀번호는 8자 이상이며 영문자와 숫자를 포함해야 합니다.")
        String newPassword // null이면 비밀번호 변경 안 함
    ) {}
