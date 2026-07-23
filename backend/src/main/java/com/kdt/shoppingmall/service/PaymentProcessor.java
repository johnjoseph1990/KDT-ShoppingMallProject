package com.kdt.shoppingmall.service;

// 결제 승인 여부를 결정하는 전략 인터페이스.
// 구현체를 교체해서 실제 PG사(토스페이먼츠) 연동 또는 테스트용 대체 구현으로 바꿀 수 있다.
public interface PaymentProcessor {

  // paymentKey/tossOrderId/amount는 결제창 SDK가 successUrl로 리다이렉트할 때 넘겨준 값이다.
  // 실제 PG사에 결제 승인을 요청해 성공 여부를 반환한다.
  boolean confirm(String paymentKey, String tossOrderId, int amount);
}
