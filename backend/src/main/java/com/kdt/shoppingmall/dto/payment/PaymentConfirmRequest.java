package com.kdt.shoppingmall.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

// 토스 결제창(SDK)이 successUrl로 리다이렉트할 때 프론트가 넘겨받는 쿼리 파라미터를 그대로 담는 DTO.
// 필드명(paymentKey/orderId/amount)은 토스페이먼츠 공식 API 명세를 그대로 따른다.
// 주의: 여기 orderId는 토스용 문자열("ORDER-1")이며, 우리 DB의 Order 기본키(Long)와 다르다.
public record PaymentConfirmRequest(
    @NotBlank(message = "paymentKey는 필수입니다.") String paymentKey,
    @NotBlank(message = "orderId는 필수입니다.") String orderId,
    @Positive(message = "결제 금액은 0보다 커야 합니다.") int amount) {}
