package com.kdt.shoppingmall.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdt.shoppingmall.config.SecurityConfig;
import com.kdt.shoppingmall.domain.order.OrderStatus;
import com.kdt.shoppingmall.domain.payment.PaymentStatus;
import com.kdt.shoppingmall.dto.order.OrderCreateRequest;
import com.kdt.shoppingmall.dto.order.OrderResponse;
import com.kdt.shoppingmall.dto.payment.PaymentResponse;
import com.kdt.shoppingmall.exception.EmptyCartException;
import com.kdt.shoppingmall.exception.GlobalExceptionHandler;
import com.kdt.shoppingmall.exception.ResourceNotFoundException;
import com.kdt.shoppingmall.security.MemberUserDetailsService;
import com.kdt.shoppingmall.service.OrderService;
import com.kdt.shoppingmall.support.WithMockMemberPrincipal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// @WebMvcTest: OrderController만 스프링 컨텍스트에 올리고, OrderService는 @MockitoBean으로
// 가짜 대체한다. "컨트롤러가 요청/응답을 올바르게 처리하는지"만 검증하는 슬라이스 테스트.
@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class OrderControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private OrderService orderService;

  @MockitoBean private MemberUserDetailsService memberUserDetailsService;

  private OrderResponse sampleResponse() {
    // 배송지 필드는 컨트롤러 테스트에서 검증 대상이 아니므로 null로 채운다
    return new OrderResponse(
        1L,
        OrderStatus.ORDERED,
        20000,
        LocalDateTime.now(),
        List.of(),
        null,
        null,
        null,
        null,
        null,
        null);
  }

  @Test
  @WithMockMemberPrincipal
  void 주문생성_인증후_201() throws Exception {
    OrderCreateRequest request =
        new OrderCreateRequest(
            List.of(1L, 2L), "홍길동", "010-1234-5678", "12345", "서울시 강남구 테헤란로 1", "101호", null);
    given(orderService.createOrder(eq(1L), any())).willReturn(sampleResponse());

    mockMvc
        .perform(
            post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("ORDERED"))
        .andExpect(jsonPath("$.totalPrice").value(20000));
  }

  @Test
  void 주문생성_미인증_401() throws Exception {
    OrderCreateRequest request =
        new OrderCreateRequest(List.of(1L, 2L), null, null, null, null, null, null);

    mockMvc
        .perform(
            post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        // 로그인 자체를 안 한 경우라 401(Unauthorized). 403은 "로그인은 했는데 권한 부족"일 때만 쓴다.
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockMemberPrincipal
  void 주문생성_빈장바구니_400() throws Exception {
    // 빈 리스트를 담은 요청 바디 (장바구니에 담을 상품이 없는 상황을 흉내)
    OrderCreateRequest request =
        new OrderCreateRequest(List.of(), null, null, null, null, null, null);
    // orderService.createOrder(...)가 호출되면 EmptyCartException을 던지도록 가짜 동작 설정
    given(orderService.createOrder(eq(1L), any()))
        .willThrow(new EmptyCartException("주문할 장바구니 항목이 없습니다."));

    mockMvc
        .perform(
            post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        // GlobalExceptionHandler.handleEmptyCart()가 EmptyCartException -> 400으로 변환
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockMemberPrincipal
  void 주문목록조회_인증후_200() throws Exception {
    given(orderService.getOrders(1L)).willReturn(List.of(sampleResponse()));

    mockMvc
        .perform(get("/api/orders"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status").value("ORDERED"));
  }

  @Test
  @WithMockMemberPrincipal
  void 주문단건조회_인증후_200() throws Exception {
    given(orderService.getOrder(1L, 1L)).willReturn(sampleResponse());

    mockMvc
        .perform(get("/api/orders/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L));
  }

  @Test
  @WithMockMemberPrincipal
  void 주문단건조회_타인소유_403() throws Exception {
    given(orderService.getOrder(1L, 1L))
        .willThrow(new AccessDeniedException("본인의 주문만 조회할 수 있습니다."));

    mockMvc.perform(get("/api/orders/1")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockMemberPrincipal
  void 주문단건조회_없는주문_404() throws Exception {
    given(orderService.getOrder(1L, 99L))
        .willThrow(new ResourceNotFoundException("주문을 찾을 수 없습니다. id=99"));

    mockMvc.perform(get("/api/orders/99")).andExpect(status().isNotFound());
  }

  @Test
  @WithMockMemberPrincipal
  void 결제_인증후_200() throws Exception {
    PaymentResponse response =
        new PaymentResponse(1L, PaymentStatus.SUCCESS, 20000, LocalDateTime.now());
    given(orderService.pay(1L, 1L)).willReturn(response);

    mockMvc
        .perform(post("/api/orders/1/pay"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"));
  }
}
