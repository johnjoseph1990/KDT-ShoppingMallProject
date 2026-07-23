package com.kdt.shoppingmall.dto.payment;

// 토스페이먼츠 결제 승인(confirm) API 요청 바디. 필드명은 토스 공식 API 명세를 그대로 따른다.
public record TossConfirmRequest(String paymentKey, String orderId, int amount) {}
