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
    List<OrderItemResponse> items,
    // 주문 시점의 배송지 정보 — 이후 회원 주소 변경과 무관하게 원본 유지
    String deliveryName,
    String deliveryPhone,
    String deliveryZipCode,
    String deliveryAddress,
    String deliveryAddressDetail,
    String deliveryNote) {

  public static OrderResponse from(Order order) {
    return new OrderResponse(
        order.getId(),
        order.getStatus(),
        order.getTotalPrice(),
        order.getCreatedAt(),
        order.getOrderItems().stream().map(OrderItemResponse::from).toList(),
        order.getDeliveryName(),
        order.getDeliveryPhone(),
        order.getDeliveryZipCode(),
        order.getDeliveryAddress(),
        order.getDeliveryAddressDetail(),
        order.getDeliveryNote());
  }
}
