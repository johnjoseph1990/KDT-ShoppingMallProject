package com.kdt.shoppingmall.domain.order;

public enum OrderStatus {
    ORDERED, PAID, SHIPPING, DELIVERED, CANCELED;

    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case ORDERED -> target == PAID || target == CANCELED;
            case PAID -> target == SHIPPING || target == CANCELED;
            case SHIPPING -> target == DELIVERED;
            default -> false;
        };
    }
}
