package com.kdt.shoppingmall.dto.order;

import com.kdt.shoppingmall.domain.order.Order;
import com.kdt.shoppingmall.domain.order.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
    Long id,
    OrderStatus status,
    int totalPrice,
    LocalDateTime createdAt,
    List<OrderItemResponse> items) {

  public static OrderResponse from(Order order) {
    return new OrderResponse(
        order.getId(),
        order.getStatus(),
        order.getTotalPrice(),
        order.getCreatedAt(),
        order.getOrderItems().stream().map(OrderItemResponse::from).toList());
  }
}
