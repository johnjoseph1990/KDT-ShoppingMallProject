package com.kdt.shoppingmall.dto.product;

import com.kdt.shoppingmall.domain.product.Product;
import com.kdt.shoppingmall.domain.product.ProductTag;
import java.time.LocalDateTime;
import java.util.List;

// /api/products/best 엔드포인트 전용 응답 DTO.
// ProductResponse와 동일한 상품 필드에 source(추천 근거)를 추가해,
// 프론트가 리뷰 기반인지 폴백인지 구분할 수 있게 한다.
public record BestProductResponse(
    Long id,
    String name,
    String description,
    int price,
    int stockQuantity,
    String imageUrl,
    List<String> tags,
    double averageRating,
    LocalDateTime createdAt,
    // "REVIEW_BEST": 리뷰 5개 이상·평균 별점순 | "SALES": 판매량 폴백 | "LATEST": 최신 폴백
    String source) {

  public static BestProductResponse from(Product product, double averageRating, String source) {
    return new BestProductResponse(
        product.getId(),
        product.getName(),
        product.getDescription(),
        product.getPrice(),
        product.getStockQuantity(),
        product.getImageUrl(),
        product.getTags().stream().map(ProductTag::getName).toList(),
        averageRating,
        product.getCreatedAt(),
        source);
  }
}
