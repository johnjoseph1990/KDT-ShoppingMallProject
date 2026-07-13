package com.kdt.shoppingmall.controller;

import com.kdt.shoppingmall.dto.order.OrderResponse;
import com.kdt.shoppingmall.dto.order.OrderStatusUpdateRequest;
import com.kdt.shoppingmall.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

  private final OrderService orderService;

  public AdminOrderController(OrderService orderService) {
    this.orderService = orderService;
  }

  @GetMapping
  public Page<OrderResponse> getAllOrders(
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return orderService.getAllOrders(pageable);
  }

  @PatchMapping("/{orderId}/status")
  public OrderResponse changeOrderStatus(
      @PathVariable Long orderId, @Valid @RequestBody OrderStatusUpdateRequest request) {
    return orderService.changeOrderStatus(orderId, request.status());
  }
}
