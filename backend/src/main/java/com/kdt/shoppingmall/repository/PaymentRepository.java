package com.kdt.shoppingmall.repository;

import com.kdt.shoppingmall.domain.order.Order;
import com.kdt.shoppingmall.domain.payment.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

  // 입금 확인 폴링 시 주문에 연결된 결제 건(paymentKey 포함)을 찾기 위함.
  Optional<Payment> findByOrder(Order order);
}
