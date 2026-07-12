package com.kdt.shoppingmall.domain.order;

public enum OrderStatus {
    ORDERED, PAID, SHIPPING, DELIVERED, CANCELED;

    public boolean canTransitionTo(OrderStatus target) {
        // TODO(human): 주문 상태 전이 규칙을 정의하세요. (현재는 모든 전이가 막혀 있습니다)
        //
        // 흐름: ORDERED(주문완료) → PAID(결제완료) → SHIPPING(배송중) → DELIVERED(배송완료)
        // 어느 단계까지 CANCELED(취소)로 갈 수 있는지도 함께 결정하세요.
        //
        // 힌트: switch (this) { case ORDERED -> target == PAID || target == CANCELED; ... default -> false; }
        return false;
    }
}
