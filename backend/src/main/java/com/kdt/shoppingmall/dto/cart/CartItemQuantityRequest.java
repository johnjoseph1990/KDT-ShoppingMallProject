package com.kdt.shoppingmall.dto.cart;

import jakarta.validation.constraints.Min;

public record CartItemQuantityRequest(@Min(1) int quantity) {}
