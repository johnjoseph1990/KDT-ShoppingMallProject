package com.kdt.shoppingmall.dto.review;

// GET /api/products/{id}/keywords 응답 DTO.
// keyword: 추출된 사전 키워드, count: 해당 상품의 전체 리뷰에서 해당 키워드가 등장한 횟수.
public record KeywordResponse(String keyword, long count) {}
