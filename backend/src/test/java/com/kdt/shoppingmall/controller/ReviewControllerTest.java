package com.kdt.shoppingmall.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdt.shoppingmall.config.SecurityConfig;
import com.kdt.shoppingmall.dto.review.ReviewRequest;
import com.kdt.shoppingmall.dto.review.ReviewResponse;
import com.kdt.shoppingmall.dto.review.ReviewUpdateRequest;
import com.kdt.shoppingmall.exception.DuplicateReviewException;
import com.kdt.shoppingmall.exception.GlobalExceptionHandler;
import com.kdt.shoppingmall.security.MemberUserDetailsService;
import com.kdt.shoppingmall.service.ReviewService;
import com.kdt.shoppingmall.support.WithMockMemberPrincipal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReviewController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ReviewControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private ReviewService reviewService;

  @MockitoBean private MemberUserDetailsService memberUserDetailsService;

  private ReviewResponse sampleResponse() {
    return new ReviewResponse(1L, 1L, "테스터", 5, "정말 좋아요!", LocalDateTime.now());
  }

  // [P1-5] getReviews가 Page를 반환하므로 응답 경로가 $.content[0]으로 변경됐다.
  @Test
  void 리뷰목록조회_인증없이_성공() throws Exception {
    given(reviewService.getReviews(eq(1L), any(Pageable.class)))
        .willReturn(new PageImpl<>(List.of(sampleResponse())));

    mockMvc
        .perform(get("/api/products/1/reviews"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].rating").value(5))
        .andExpect(jsonPath("$.content[0].memberName").value("테스터"));
  }

  @Test
  @WithMockMemberPrincipal
  void 리뷰작성_인증후_201() throws Exception {
    ReviewRequest request = new ReviewRequest(5, "정말 좋아요!");
    given(reviewService.createReview(any(), eq(1L), any())).willReturn(sampleResponse());

    mockMvc
        .perform(
            post("/api/products/1/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.rating").value(5));
  }

  @Test
  void 리뷰작성_미인증_401() throws Exception {
    ReviewRequest request = new ReviewRequest(5, "정말 좋아요!");

    mockMvc
        .perform(
            post("/api/products/1/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        // 로그인 자체를 안 한 경우라 401(Unauthorized). 403은 "로그인은 했는데 권한 부족"일 때만 쓴다.
        .andExpect(status().isUnauthorized());
  }

  // [P0-1] 로그인은 했지만 해당 상품을 구매한 적 없으면 서비스가 AccessDeniedException을 던져 403 반환.
  @Test
  @WithMockMemberPrincipal
  void 리뷰작성_미구매자_403() throws Exception {
    ReviewRequest request = new ReviewRequest(5, "좋아요!");
    given(reviewService.createReview(any(), eq(1L), any()))
        .willThrow(new AccessDeniedException("구매자만 리뷰를 작성할 수 있습니다."));

    mockMvc
        .perform(
            post("/api/products/1/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("구매자만 리뷰를 작성할 수 있습니다."));
  }

  @Test
  @WithMockMemberPrincipal
  void 리뷰작성_중복_409() throws Exception {
    ReviewRequest request = new ReviewRequest(3, "두 번째 리뷰");
    given(reviewService.createReview(any(), eq(1L), any()))
        .willThrow(new DuplicateReviewException("이미 리뷰를 작성한 상품입니다."));

    mockMvc
        .perform(
            post("/api/products/1/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("이미 리뷰를 작성한 상품입니다."));
  }

  @Test
  @WithMockMemberPrincipal
  void 리뷰작성_유효성실패_400() throws Exception {
    ReviewRequest request = new ReviewRequest(0, "");

    mockMvc
        .perform(
            post("/api/products/1/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockMemberPrincipal
  void 리뷰삭제_성공_204() throws Exception {
    mockMvc.perform(delete("/api/products/1/reviews/1")).andExpect(status().isNoContent());
  }

  // 본인이 로그인 상태에서 수정 요청하면 200 응답.
  @Test
  @WithMockMemberPrincipal
  void 리뷰수정_본인_200() throws Exception {
    ReviewUpdateRequest request = new ReviewUpdateRequest(4, "수정된 리뷰 내용입니다.");
    given(reviewService.updateReview(any(), eq(1L), any())).willReturn(sampleResponse());

    mockMvc
        .perform(
            put("/api/products/1/reviews/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rating").value(5));
  }

  // 로그인 없이 수정 요청하면 401.
  @Test
  void 리뷰수정_미인증_401() throws Exception {
    ReviewUpdateRequest request = new ReviewUpdateRequest(3, "무단 수정 시도");

    mockMvc
        .perform(
            put("/api/products/1/reviews/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }
}
