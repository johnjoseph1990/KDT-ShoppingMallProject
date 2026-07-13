package com.kdt.shoppingmall.dto.review;

import com.kdt.shoppingmall.domain.review.Review;
import java.time.LocalDateTime;

public record ReviewResponse(
    Long id,
    Long memberId,
    String memberName,
    int rating,
    String content,
    LocalDateTime createdAt) {

  public static ReviewResponse from(Review review) {
    return new ReviewResponse(
        review.getId(),
        review.getMember().getId(),
        review.getMember().getName(),
        review.getRating(),
        review.getContent(),
        review.getCreatedAt());
  }
}
