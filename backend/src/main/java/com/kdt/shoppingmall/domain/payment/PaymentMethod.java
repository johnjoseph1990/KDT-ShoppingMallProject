package com.kdt.shoppingmall.domain.payment;

// 결제수단 구분. 카드결제는 승인 즉시 완료되지만, 가상계좌는 발급 후 실제 입금까지
// 시간차가 있어 Payment.status(WAITING_FOR_DEPOSIT)와 함께 봐야 흐름이 이해된다.
public enum PaymentMethod {
  CARD,
  VIRTUAL_ACCOUNT
}
