package com.kdt.shoppingmall.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;

// 주문 생성 요청 DTO. cartItemIds가 null이면 서비스에서 해당 회원의 전체 장바구니를 사용한다.
public record OrderCreateRequest(
    List<Long> cartItemIds,
    // 배송지 핵심 4개 필드는 필수값이다. 나머지(상세주소, 메모)는 선택.
    @NotBlank(message = "받는 분 이름을 입력해주세요.") String deliveryName,
    // @Pattern: 010-1234-5678 형식만 허용해 임의 문자열 저장을 막는다
    @NotBlank(message = "연락처를 입력해주세요.")
        @Pattern(
            regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$",
            message = "연락처 형식이 올바르지 않습니다. (예: 010-1234-5678)")
        String deliveryPhone,
    // @Pattern: 5자리 숫자 우편번호만 허용한다
    @NotBlank(message = "우편번호를 입력해주세요.")
        @Pattern(regexp = "^\\d{5}$", message = "우편번호는 5자리 숫자여야 합니다.")
        String deliveryZipCode,
    @NotBlank(message = "주소를 입력해주세요.") String deliveryAddress,
    String deliveryAddressDetail,
    String deliveryNote) {}
