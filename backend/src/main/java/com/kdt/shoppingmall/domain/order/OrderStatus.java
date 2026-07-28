package com.kdt.shoppingmall.domain.order;

public enum OrderStatus {
  ORDERED,
  // 가상계좌를 발급받고 실제 입금을 기다리는 중. 카드결제는 이 상태를 거치지 않고
  // ORDERED에서 바로 PAID로 간다.
  WAITING_FOR_DEPOSIT,
  PAID,
  SHIPPING,
  DELIVERED,
  CANCELED;

  public boolean canTransitionTo(OrderStatus target) {
    return switch (this) {
      case ORDERED -> target == PAID || target == CANCELED || target == WAITING_FOR_DEPOSIT;
      case WAITING_FOR_DEPOSIT -> target == PAID || target == CANCELED;
      case PAID -> target == SHIPPING || target == CANCELED;
      case SHIPPING -> target == DELIVERED;
      default -> false;
    };
  }
}
