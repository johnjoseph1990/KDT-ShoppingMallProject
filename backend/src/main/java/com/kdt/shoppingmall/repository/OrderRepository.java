package com.kdt.shoppingmall.repository;

import com.kdt.shoppingmall.domain.order.Order;
import com.kdt.shoppingmall.domain.order.OrderStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

  List<Order> findByMemberIdOrderByCreatedAtDesc(Long memberId);

  // 메서드 이름만으로 "WHERE status = ?" 쿼리를 Spring Data JPA가 자동 생성해준다.
  Page<Order> findByStatus(OrderStatus status, Pageable pageable);
}
