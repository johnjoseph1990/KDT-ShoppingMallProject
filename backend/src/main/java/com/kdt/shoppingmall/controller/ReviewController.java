package com.kdt.shoppingmall.controller;

import com.kdt.shoppingmall.dto.review.ReviewRequest;
import com.kdt.shoppingmall.dto.review.ReviewResponse;
import com.kdt.shoppingmall.security.MemberPrincipal;
import com.kdt.shoppingmall.service.ReviewService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

  @PostMapping
  public ResponseEntity<ReviewResponse> create(
      @PathVariable Long productId,
      @Valid @RequestBody ReviewRequest request,
      @AuthenticationPrincipal MemberPrincipal principal) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(reviewService.createReview(principal.getMember().getId(), productId, request));
  }

  @GetMapping
  public List<ReviewResponse> getReviews(@PathVariable Long productId) {
    return reviewService.getReviews(productId);
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
