package com.kdt.shoppingmall.dto.order;

import com.kdt.shoppingmall.domain.order.OrderItem;

public record OrderItemResponse(
        Long productId,
        String productName,
        int orderPrice,
        int quantity
) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getOrderPrice(),
                item.getQuantity()
        );
    }
}
