package com.kdt.shoppingmall.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdt.shoppingmall.config.SecurityConfig;
import com.kdt.shoppingmall.domain.order.OrderStatus;
import com.kdt.shoppingmall.dto.order.OrderResponse;
import com.kdt.shoppingmall.dto.order.OrderStatusUpdateRequest;
import com.kdt.shoppingmall.exception.GlobalExceptionHandler;
import com.kdt.shoppingmall.exception.InvalidOrderStatusException;
import com.kdt.shoppingmall.security.MemberUserDetailsService;
import com.kdt.shoppingmall.service.OrderService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminOrderController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AdminOrderControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private OrderService orderService;

  @MockitoBean private MemberUserDetailsService memberUserDetailsService;

  private OrderResponse sampleResponse(OrderStatus status) {
    // 배송지 필드는 관리자 주문 테스트에서 검증 대상이 아니므로 null로 채운다
    return new OrderResponse(
        1L, status, 10000, LocalDateTime.now(), List.of(), null, null, null, null, null, null);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void 전체주문조회_ADMIN_200() throws Exception {
    Page<OrderResponse> page = new PageImpl<>(List.of(sampleResponse(OrderStatus.ORDERED)));
    given(orderService.getAllOrders(eq(null), any(Pageable.class))).willReturn(page);

    mockMvc
        .perform(get("/api/admin/orders"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].status").value("ORDERED"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void 전체주문조회_상태필터_200() throws Exception {
    Page<OrderResponse> page = new PageImpl<>(List.of(sampleResponse(OrderStatus.PAID)));
    given(orderService.getAllOrders(eq(OrderStatus.PAID), any(Pageable.class))).willReturn(page);

    mockMvc
        .perform(get("/api/admin/orders").param("status", "PAID"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].status").value("PAID"));
  }

  @Test
  void 전체주문조회_미인증_401() throws Exception {
    // 로그인 자체를 안 한 경우라 401(Unauthorized). 403은 "로그인은 했는데 권한 부족"일 때만 쓴다.
    mockMvc.perform(get("/api/admin/orders")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void 주문상태변경_ADMIN_200() throws Exception {
    OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(OrderStatus.SHIPPING);
    given(orderService.changeOrderStatus(eq(1L), eq(OrderStatus.SHIPPING)))
        .willReturn(sampleResponse(OrderStatus.SHIPPING));

    mockMvc
        .perform(
            patch("/api/admin/orders/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SHIPPING"));
  }

  @Test
  void 주문상태변경_미인증_401() throws Exception {
    OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(OrderStatus.SHIPPING);

    mockMvc
        .perform(
            patch("/api/admin/orders/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        // 로그인 자체를 안 한 경우라 401(Unauthorized). 403은 "로그인은 했는데 권한 부족"일 때만 쓴다.
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void 주문상태변경_잘못된전이_400() throws Exception {
    OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(OrderStatus.DELIVERED);
    given(orderService.changeOrderStatus(eq(1L), eq(OrderStatus.DELIVERED)))
        .willThrow(new InvalidOrderStatusException("주문 상태를 ORDERED에서 DELIVERED로 변경할 수 없습니다."));

    mockMvc
        .perform(
            patch("/api/admin/orders/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "USER")
  void 전체주문조회_USER권한_403() throws Exception {
    // 로그인은 했지만(USER) ADMIN 권한이 없으므로 403.
    // SecurityConfig의 /api/admin/** → hasRole("ADMIN") 규칙이 실제로 USER를 막는지 검증.
    mockMvc.perform(get("/api/admin/orders")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "USER")
  void 주문상태변경_USER권한_403() throws Exception {
    // 로그인은 했지만(USER) ADMIN 권한이 없으므로 403.
    OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(OrderStatus.SHIPPING);

    mockMvc
        .perform(
            patch("/api/admin/orders/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }
}
