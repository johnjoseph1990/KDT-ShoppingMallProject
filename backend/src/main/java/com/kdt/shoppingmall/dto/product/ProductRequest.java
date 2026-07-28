package com.kdt.shoppingmall.dto.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ProductRequest(
    // @Size: 비정상적으로 긴 문자열로 DB 부하를 주는 것을 방지한다
    @NotBlank @Size(max = 100) String name,
    @Size(max = 2000) String description,
    @Min(0) int price,
    @Min(0) int stockQuantity,
    String imageUrl,
    List<String> tags) {}
