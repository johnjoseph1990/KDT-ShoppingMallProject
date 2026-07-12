package com.kdt.shoppingmall.dto.cart;

import com.kdt.shoppingmall.domain.cart.CartItem;

public record CartItemResponse(
        Long id,
        Long productId,
        String productName,
        int price,
        int quantity
) {

    public static CartItemResponse from(CartItem cartItem) {
        return new CartItemResponse(
                cartItem.getId(),
                cartItem.getProduct().getId(),
                cartItem.getProduct().getName(),
                cartItem.getProduct().getPrice(),
                cartItem.getQuantity()
        );
    }
}
