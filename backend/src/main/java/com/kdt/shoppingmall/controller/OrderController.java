package com.kdt.shoppingmall.controller;

import com.kdt.shoppingmall.dto.order.OrderCreateRequest;
import com.kdt.shoppingmall.dto.order.OrderResponse;
import com.kdt.shoppingmall.dto.payment.PaymentResponse;
import com.kdt.shoppingmall.security.MemberPrincipal;
import com.kdt.shoppingmall.service.OrderService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

  private final OrderService orderService;

  public OrderController(OrderService orderService) {
    this.orderService = orderService;
  }

  @PostMapping
  public ResponseEntity<OrderResponse> createOrder(
      @AuthenticationPrincipal MemberPrincipal principal,
      @RequestBody(required = false) OrderCreateRequest request) {
    OrderResponse response = orderService.createOrder(principal.getMember().getId(), request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  public List<OrderResponse> getOrders(@AuthenticationPrincipal MemberPrincipal principal) {
    return orderService.getOrders(principal.getMember().getId());
  }

  @GetMapping("/{orderId}")
  public OrderResponse getOrder(
      @AuthenticationPrincipal MemberPrincipal principal, @PathVariable Long orderId) {
    return orderService.getOrder(principal.getMember().getId(), orderId);
  }

  @PostMapping("/{orderId}/pay")
  public PaymentResponse pay(
      @AuthenticationPrincipal MemberPrincipal principal, @PathVariable Long orderId) {
    return orderService.pay(principal.getMember().getId(), orderId);
  }
}
