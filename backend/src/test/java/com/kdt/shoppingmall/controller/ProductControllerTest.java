package com.kdt.shoppingmall.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdt.shoppingmall.config.SecurityConfig;
import com.kdt.shoppingmall.dto.product.BestProductResponse;
import com.kdt.shoppingmall.dto.product.ProductRequest;
import com.kdt.shoppingmall.dto.product.ProductResponse;
import com.kdt.shoppingmall.dto.review.KeywordResponse;
import com.kdt.shoppingmall.exception.GlobalExceptionHandler;
import com.kdt.shoppingmall.exception.ResourceNotFoundException;
import com.kdt.shoppingmall.security.MemberUserDetailsService;
import com.kdt.shoppingmall.service.ProductService;
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

@WebMvcTest(ProductController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ProductControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private ProductService productService;

  @MockitoBean private MemberUserDetailsService memberUserDetailsService;

  private ProductResponse sampleResponse() {
    // viewCount(0)가 추가되어 10번째 인자 추가됨
    return new ProductResponse(
        1L, "상품A", "설명", 10000, 100, null, List.of(), 0.0, 0, LocalDateTime.now());
  }

  @Test
  void 상품목록조회_인증없이_성공() throws Exception {
    Page<ProductResponse> page = new PageImpl<>(List.of(sampleResponse()));
    given(productService.search(isNull(), isNull(), isNull(), any(Pageable.class)))
        .willReturn(page);

    mockMvc
        .perform(get("/api/products"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name").value("상품A"))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void 상품목록조회_키워드검색() throws Exception {
    Page<ProductResponse> page = new PageImpl<>(List.of(sampleResponse()));
    given(productService.search(eq("상품"), isNull(), isNull(), any(Pageable.class)))
        .willReturn(page);

    mockMvc
        .perform(get("/api/products").param("keyword", "상품"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name").value("상품A"));
  }

  @Test
  void 상품목록조회_태그필터() throws Exception {
    Page<ProductResponse> page = new PageImpl<>(List.of(sampleResponse()));
    given(productService.search(isNull(), eq("신발"), isNull(), any(Pageable.class)))
        .willReturn(page);

    mockMvc
        .perform(get("/api/products").param("tag", "신발"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name").value("상품A"));
  }

  // [별점 필터] 쿼리스트링 "minRating=4"가 Double 4.0으로 변환돼 서비스에 전달되는지 검증한다.
  // 컨트롤러 슬라이스 테스트의 역할은 "HTTP 요청 → 자바 파라미터 변환"이 맞는지 확인하는 것.
  @Test
  void 상품목록조회_최소별점필터() throws Exception {
    Page<ProductResponse> page = new PageImpl<>(List.of(sampleResponse()));
    given(productService.search(isNull(), isNull(), eq(4.0), any(Pageable.class))).willReturn(page);

    mockMvc
        .perform(get("/api/products").param("minRating", "4"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name").value("상품A"));
  }

  // [P1-3] findBestProducts가 BestProductResponse를 반환하므로 목 타입을 맞춘다.
  @Test
  void 베스트상품조회_인증없이_성공() throws Exception {
    BestProductResponse bestResponse =
        new BestProductResponse(
            1L,
            "상품A",
            "설명",
            10000,
            100,
            null,
            List.of(),
            0.0,
            0,
            LocalDateTime.now(),
            "REVIEW_BEST");
    Page<BestProductResponse> page = new PageImpl<>(List.of(bestResponse));
    given(productService.findBestProducts(any(Pageable.class))).willReturn(page);

    mockMvc
        .perform(get("/api/products/best"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name").value("상품A"))
        .andExpect(jsonPath("$.content[0].source").value("REVIEW_BEST"));
  }

  @Test
  void 상품단건조회_인증없이_성공() throws Exception {
    given(productService.findById(1L)).willReturn(sampleResponse());

    mockMvc
        .perform(get("/api/products/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("상품A"))
        .andExpect(jsonPath("$.price").value(10000));
  }

  @Test
  void 상품단건조회_없는상품_404() throws Exception {
    given(productService.findById(99L))
        .willThrow(new ResourceNotFoundException("상품을 찾을 수 없습니다. id=99"));

    mockMvc
        .perform(get("/api/products/99"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("상품을 찾을 수 없습니다. id=99"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void 상품등록_ADMIN_201() throws Exception {
    ProductRequest request = new ProductRequest("상품A", "설명", 10000, 100, null, null);
    given(productService.create(any())).willReturn(sampleResponse());

    mockMvc
        .perform(
            post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("상품A"));
  }

  @Test
  @WithMockUser(roles = "USER")
  void 상품등록_USER권한_403() throws Exception {
    ProductRequest request = new ProductRequest("상품A", "설명", 10000, 100, null, null);

    mockMvc
        .perform(
            post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        // 로그인은 했지만(USER) ADMIN 권한이 없으므로 403. SecurityConfig의
        // hasRole("ADMIN") 규칙이 실제로 USER를 막는지는 이 "실패해야 하는" 케이스로만 증명된다.
        .andExpect(status().isForbidden());
  }

  @Test
  void 상품등록_미인증_401() throws Exception {
    ProductRequest request = new ProductRequest("상품A", "설명", 10000, 100, null, null);

    mockMvc
        .perform(
            post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        // 로그인 자체를 안 한 경우라 401(Unauthorized). 403은 "로그인은 했는데 권한 부족"일 때만 쓴다.
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void 상품수정_ADMIN_200() throws Exception {
    ProductRequest request = new ProductRequest("수정상품", "수정설명", 9000, 50, null, null);
    ProductResponse updated =
        new ProductResponse(
            1L, "수정상품", "수정설명", 9000, 50, null, List.of(), 0.0, 0, LocalDateTime.now());
    given(productService.update(eq(1L), any())).willReturn(updated);

    mockMvc
        .perform(
            put("/api/products/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("수정상품"));
  }

  // 상품 키워드 조회 — 빈도 내림차순으로 반환되어야 한다.
  @Test
  void 상품키워드조회_인증없이_성공() throws Exception {
    // limit은 int 프리미티브라 anyInt()를 사용한다.
    given(productService.getKeywords(eq(1L), anyInt()))
        .willReturn(List.of(new KeywordResponse("신선", 5L), new KeywordResponse("맛있", 3L)));

    mockMvc
        .perform(get("/api/products/1/keywords"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].keyword").value("신선"))
        .andExpect(jsonPath("$[0].count").value(5))
        .andExpect(jsonPath("$[1].keyword").value("맛있"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void 상품삭제_ADMIN_204() throws Exception {
    mockMvc.perform(delete("/api/products/1")).andExpect(status().isNoContent());
  }

  @Test
  void 상품추천조회_인증없이_성공() throws Exception {
    // GET /api/products/{id}/recommendations: 인증 없이도 같은 태그 추천 상품 목록을 볼 수 있어야 한다.
    // SecurityConfig에서 GET /api/products/** 는 permitAll이므로 인증 없이 200이 와야 한다.
    given(productService.getRecommendations(1L)).willReturn(List.of(sampleResponse()));

    mockMvc
        .perform(get("/api/products/1/recommendations"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("상품A"));
  }

  @Test
  void 상품추천조회_없는상품_404() throws Exception {
    given(productService.getRecommendations(99L))
        .willThrow(new ResourceNotFoundException("상품을 찾을 수 없습니다. id=99"));

    mockMvc.perform(get("/api/products/99/recommendations")).andExpect(status().isNotFound());
  }

  // ─────────────────────────────────────────────────────────────────────────
  // 아래 2개는 "스프링 MVC가 스스로 만들어내는 예외"의 응답 계약을 검증한다.
  // 우리 서비스 코드가 던지는 예외(ResourceNotFound 등)와 달리, 요청이 서비스에
  // 도달하기도 전에 MVC 단계에서 터지는 예외라서 productService를 mock하지 않는다.
  //
  // 왜 필요한가: GlobalExceptionHandler에 @ExceptionHandler(Exception.class)가 있는데,
  // 이 핸들러를 찾는 ExceptionHandlerExceptionResolver가 스프링 기본 변환기인
  // DefaultHandlerExceptionResolver보다 "먼저" 실행된다. 그래서 스프링이 400/405로
  // 바꿔주던 예외까지 catch-all이 가로채 500으로 내려보낼 수 있다.
  // (같은 문제로 AuthenticationException이 500이 되던 적이 있다 — GlobalExceptionHandler 36~40행 주석)
  // ─────────────────────────────────────────────────────────────────────────

  @Test
  void 경로변수_타입이_틀리면_400을_반환한다() throws Exception {
    // GET /api/products/{id}의 id는 Long인데 "abc"가 들어오면
    // 스프링이 MethodArgumentTypeMismatchException을 던진다 → 잘못된 요청이므로 400이어야 한다.
    // SecurityConfig에서 GET /api/products/** 는 permitAll이라 인증 없이 프로브할 수 있다.
    mockMvc.perform(get("/api/products/abc")).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "USER")
  void 지원하지_않는_HTTP_메서드는_405를_반환한다() throws Exception {
    // ProductController에는 PATCH 매핑이 없다 → HttpRequestMethodNotSupportedException.
    // @WithMockUser가 필수인 이유: 인증은 DispatcherServlet보다 앞선 필터체인에서 처리된다.
    // PATCH는 SecurityConfig의 anyRequest().authenticated()에 걸리므로, 인증 없이 쏘면
    // 401에서 끊겨 우리가 확인하려는 405 지점까지 도달조차 못 한다.
    mockMvc.perform(patch("/api/products/1")).andExpect(status().isMethodNotAllowed());
  }
}
