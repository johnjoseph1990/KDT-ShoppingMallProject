package com.kdt.shoppingmall.dto.order;

import java.util.List;

// 주문 생성 요청 DTO. cartItemIds가 null이면 서비스에서 해당 회원의 전체 장바구니를 사용한다.
public record OrderCreateRequest(
    List<Long> cartItemIds,
    String deliveryName,
    String deliveryPhone,
    String deliveryZipCode,
    String deliveryAddress,
    String deliveryAddressDetail,
    String deliveryNote) {}
