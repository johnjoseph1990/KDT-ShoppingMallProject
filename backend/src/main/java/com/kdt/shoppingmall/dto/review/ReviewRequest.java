package com.kdt.shoppingmall.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// @Size(max=1000): 수 MB 문자열로 DB/메모리에 부하를 주는 DoS를 방지한다
public record ReviewRequest(
    @Min(1) @Max(5) int rating, @NotBlank @Size(max = 1000) String content) {}
