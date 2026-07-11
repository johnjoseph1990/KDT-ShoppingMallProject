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
        LocalDateTime createdAt
) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getImageUrl(),
                product.getTags().stream().map(tag -> tag.getName()).toList(),
                product.getCreatedAt()
        );
    }
}
