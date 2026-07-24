package com.kdt.shoppingmall.controller;

import com.kdt.shoppingmall.dto.review.ReviewRequest;
import com.kdt.shoppingmall.dto.review.ReviewResponse;
import com.kdt.shoppingmall.dto.review.ReviewUpdateRequest;
import com.kdt.shoppingmall.security.MemberPrincipal;
import com.kdt.shoppingmall.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
public class ReviewController {

  private final ReviewService reviewService;

  public ReviewController(ReviewService reviewService) {
    this.reviewService = reviewService;
  }

  // [P0-1] 구매자 권한 검증은 ReviewService.createReview에서 수행한다.
  // 미구매자는 AccessDeniedException(→ 403)이 던져진다.
  @PostMapping
  public ResponseEntity<ReviewResponse> create(
      @PathVariable Long productId,
      @Valid @RequestBody ReviewRequest request,
      @AuthenticationPrincipal MemberPrincipal principal) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(reviewService.createReview(principal.getMember().getId(), productId, request));
  }

  // [P1-5] List → Page: 기본 20개씩, 최신순으로 페이징해 반환한다.
  // 클라이언트는 ?page=0&size=20 파라미터로 제어할 수 있다.
  @GetMapping
  public Page<ReviewResponse> getReviews(
      @PathVariable Long productId,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return reviewService.getReviews(productId, pageable);
  }

  @PutMapping("/{reviewId}")
  public ResponseEntity<ReviewResponse> update(
      @PathVariable Long productId,
      @PathVariable Long reviewId,
      @Valid @RequestBody ReviewUpdateRequest request,
      @AuthenticationPrincipal MemberPrincipal principal) {
    return ResponseEntity.ok(
        reviewService.updateReview(principal.getMember().getId(), reviewId, request));
  }

  @DeleteMapping("/{reviewId}")
  public ResponseEntity<Void> delete(
      @PathVariable Long productId,
      @PathVariable Long reviewId,
      @AuthenticationPrincipal MemberPrincipal principal) {
    reviewService.deleteReview(principal.getMember().getId(), reviewId);
    return ResponseEntity.noContent().build();
  }
}
