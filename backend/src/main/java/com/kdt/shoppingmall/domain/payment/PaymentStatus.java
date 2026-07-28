package com.kdt.shoppingmall.domain.payment;

public enum PaymentStatus {
  SUCCESS,
  FAILED,
  // 가상계좌 발급은 됐지만 아직 실제 입금이 확인되지 않은 상태.
  WAITING_FOR_DEPOSIT
}
