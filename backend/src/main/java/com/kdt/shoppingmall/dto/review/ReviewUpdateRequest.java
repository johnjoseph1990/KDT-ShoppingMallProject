package com.kdt.shoppingmall.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

// 리뷰 수정 요청 DTO.
// ReviewRequest와 동일한 필드지만 "수정" 의도를 명확히 표현하기 위해 별도 클래스로 분리한다.
// 역할별 DTO 분리 원칙: Request(입력)는 용도마다 따로 만들어 API 계약이 독립적으로 변경 가능하게 한다.
public record ReviewUpdateRequest(@Min(1) @Max(5) int rating, @NotBlank String content) {}
