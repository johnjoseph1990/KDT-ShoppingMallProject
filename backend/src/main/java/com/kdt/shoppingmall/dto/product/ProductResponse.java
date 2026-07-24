package com.kdt.shoppingmall.dto.product;

import com.kdt.shoppingmall.domain.product.Product;
import java.time.LocalDateTime;
import java.util.List;

public record ProductResponse(
    Long id,
    String name,
    String description,
    int price,
    int stockQuantity,
    String imageUrl,
    List<String> tags,
    double averageRating,
    // 상품 상세 페이지 조회 횟수
    int viewCount,
    LocalDateTime createdAt) {

  public static ProductResponse from(Product product, Double averageRating) {
    return new ProductResponse(
        product.getId(),
        product.getName(),
        product.getDescription(),
        product.getPrice(),
        product.getStockQuantity(),
        product.getImageUrl(),
        product.getTags().stream().map(tag -> tag.getName()).toList(),
        // 리뷰가 없으면 AVG 쿼리 결과가 null이라, 화면에는 0.0으로 표시
        averageRating == null ? 0.0 : averageRating,
        product.getViewCount(),
        product.getCreatedAt());
  }
}
