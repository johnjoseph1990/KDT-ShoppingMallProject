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
import org.springframework.http.MediaType;
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

  @Test
  void 리뷰목록조회_인증없이_성공() throws Exception {
    given(reviewService.getReviews(1L)).willReturn(List.of(sampleResponse()));

    mockMvc
        .perform(get("/api/products/1/reviews"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].rating").value(5))
        .andExpect(jsonPath("$[0].memberName").value("테스터"));
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
  void 리뷰작성_미인증_403() throws Exception {
    ReviewRequest request = new ReviewRequest(5, "정말 좋아요!");

    mockMvc
        .perform(
            post("/api/products/1/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
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
}
