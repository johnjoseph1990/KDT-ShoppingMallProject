package com.kdt.shoppingmall.dto.order;

import java.util.List;

public record OrderCreateRequest(
        List<Long> cartItemIds
) {
}
