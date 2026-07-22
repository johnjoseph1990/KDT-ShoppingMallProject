package com.kdt.shoppingmall.dto.order;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

// 주문 생성 요청 DTO. cartItemIds가 null이면 서비스에서 해당 회원의 전체 장바구니를 사용한다.
public record OrderCreateRequest(
    List<Long> cartItemIds,
    // 배송지 핵심 4개 필드는 필수값이다. 나머지(상세주소, 메모)는 선택.
    @NotBlank(message = "받는 분 이름을 입력해주세요.") String deliveryName,
    @NotBlank(message = "연락처를 입력해주세요.") String deliveryPhone,
    @NotBlank(message = "우편번호를 입력해주세요.") String deliveryZipCode,
    @NotBlank(message = "주소를 입력해주세요.") String deliveryAddress,
    String deliveryAddressDetail,
    String deliveryNote) {}
