package com.kdt.shoppingmall.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdt.shoppingmall.config.SecurityConfig;
import com.kdt.shoppingmall.dto.cart.CartItemQuantityRequest;
import com.kdt.shoppingmall.dto.cart.CartItemRequest;
import com.kdt.shoppingmall.dto.cart.CartItemResponse;
import com.kdt.shoppingmall.exception.GlobalExceptionHandler;
import com.kdt.shoppingmall.exception.ResourceNotFoundException;
import com.kdt.shoppingmall.security.MemberUserDetailsService;
import com.kdt.shoppingmall.service.CartService;
import com.kdt.shoppingmall.support.WithMockMemberPrincipal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// @WebMvcTest: Controller 계층만 띄우는 슬라이스 테스트. Service/Repository는 실제로 뜨지 않고
// @MockitoBean으로 가짜 객체를 주입받아, "컨트롤러가 요청을 올바르게 처리하는지"만 검증한다.
@WebMvcTest(CartController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class CartControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  // CartService를 실제로 만들지 않고 가짜(Mock)로 대체한다. given()으로 "이렇게 호출되면
  // 이 값을 반환해라"를 미리 정해두고, 그 상황에서 컨트롤러가 응답을 올바르게 만드는지만 본다.
  @MockitoBean private CartService cartService;

  @MockitoBean private MemberUserDetailsService memberUserDetailsService;

  private CartItemResponse sampleResponse() {
    return new CartItemResponse(1L, 10L, "상품A", 10000, 2);
  }

  @Test
  @WithMockMemberPrincipal
  void 장바구니추가_인증후_201() throws Exception {
    CartItemRequest request = new CartItemRequest(10L, 2);
    given(cartService.addItem(eq(1L), any())).willReturn(sampleResponse());

    mockMvc
        .perform(
            post("/api/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.productName").value("상품A"))
        .andExpect(jsonPath("$.quantity").value(2));
  }

  @Test
  void 장바구니추가_미인증_401() throws Exception {
    CartItemRequest request = new CartItemRequest(10L, 2);

    mockMvc
        .perform(
            post("/api/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        // 로그인 자체를 안 한 경우라 401(Unauthorized). 403은 "로그인은 했는데 권한 부족"일 때만 쓴다.
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockMemberPrincipal
  void 장바구니추가_유효성실패_400() throws Exception {
    // quantity는 @Min(1)인데 0을 보내서 검증 실패(400)를 유도한다.
    CartItemRequest request = new CartItemRequest(10L, 0);

    mockMvc
        .perform(
            post("/api/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockMemberPrincipal
  void 장바구니조회_인증후_200() throws Exception {
    given(cartService.getCart(1L)).willReturn(List.of(sampleResponse()));

    mockMvc
        .perform(get("/api/cart"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].productName").value("상품A"));
  }

  @Test
  @WithMockMemberPrincipal
  void 장바구니수량수정_인증후_200() throws Exception {
    CartItemQuantityRequest request = new CartItemQuantityRequest(5);
    CartItemResponse updated = new CartItemResponse(1L, 10L, "상품A", 10000, 5);
    given(cartService.updateQuantity(eq(1L), eq(1L), eq(5))).willReturn(updated);

    mockMvc
        .perform(
            put("/api/cart/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quantity").value(5));
  }

  @Test
  @WithMockMemberPrincipal
  void 장바구니수량수정_타인소유_403() throws Exception {
    CartItemQuantityRequest request = new CartItemQuantityRequest(5);
    // updateQuantity는 반환값이 있는 메서드라, given(...).willThrow(...) 형태로
    // "이 메서드가 호출되면 예외를 던져라"를 미리 정해둘 수 있다.
    given(cartService.updateQuantity(eq(1L), eq(1L), eq(5)))
        .willThrow(new AccessDeniedException("본인의 장바구니만 수정할 수 있습니다."));

    mockMvc
        .perform(
            put("/api/cart/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockMemberPrincipal
  void 장바구니삭제_인증후_204() throws Exception {
    mockMvc.perform(delete("/api/cart/1")).andExpect(status().isNoContent());
  }

  @Test
  @WithMockMemberPrincipal
  void 장바구니삭제_없는항목_404() throws Exception {
    willThrow(new ResourceNotFoundException("장바구니 항목을 찾을 수 없습니다. id=99"))
        .given(cartService)
        .removeItem(eq(1L), eq(99L));

    mockMvc.perform(delete("/api/cart/99")).andExpect(status().isNotFound());
  }
}
