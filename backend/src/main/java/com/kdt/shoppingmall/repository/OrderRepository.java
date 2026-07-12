package com.kdt.shoppingmall.repository;

import com.kdt.shoppingmall.domain.order.Order;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}
