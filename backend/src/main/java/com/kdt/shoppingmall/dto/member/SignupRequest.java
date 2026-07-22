package com.kdt.shoppingmall.dto.member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SignupRequest(
    @NotBlank @Email String email,
    // 8자 이상이며 영문자와 숫자를 반드시 각각 1자 이상 포함해야 한다.
    // (?=.*[A-Za-z]) = 영문자가 최소 1개, (?=.*\d) = 숫자가 최소 1개
    @NotBlank
        @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
            message = "비밀번호는 8자 이상이며 영문자와 숫자를 포함해야 합니다.")
        String password,
    @NotBlank String name) {}
