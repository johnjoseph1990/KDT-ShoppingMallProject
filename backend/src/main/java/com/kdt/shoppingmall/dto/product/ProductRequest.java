package com.kdt.shoppingmall.dto.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ProductRequest(
        @NotBlank String name,
        String description,
        @Min(0) int price,
        @Min(0) int stockQuantity,
        String imageUrl,
        List<String> tags
) {
}
