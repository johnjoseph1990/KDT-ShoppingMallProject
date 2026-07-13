package com.kdt.shoppingmall.dto.order;

import com.kdt.shoppingmall.domain.order.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(@NotNull OrderStatus status) {}
