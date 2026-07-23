package com.kdt.shoppingmall.service;

// 결제 성공 여부를 결정하는 전략 인터페이스.
// 구현체를 교체해서 실제 PG사 연동 또는 테스트용 Mock으로 바꿀 수 있다.
public interface PaymentProcessor {
  boolean isSuccess();
}
