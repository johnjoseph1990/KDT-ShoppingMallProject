package com.kdt.shoppingmall.repository;

import com.kdt.shoppingmall.domain.order.OrderItem;
import com.kdt.shoppingmall.domain.order.OrderStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

// OrderItem(주문 항목)에 대한 데이터베이스 접근 인터페이스.
// JpaRepository를 상속하면 기본 CRUD 메서드가 자동으로 제공된다.
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

  // 특정 회원이 특정 상품을 '결제 완료(PAID) 이상' 상태의 주문으로 구매한 적 있는지 확인한다.
  // 리뷰 작성 시 "구매자만 작성 가능" 규칙을 검증하는 용도.
  //
  // ORDERED(주문만 생성됨)·CANCELED(취소)는 실제 결제가 이뤄지지 않아 구매로 보지 않는다.
  // 전달하는 statuses 값: [PAID, SHIPPING, DELIVERED]
  //
  // Spring Data JPA가 메서드 이름으로 아래 JPQL을 자동 생성한다:
  //   SELECT COUNT(oi) > 0 FROM OrderItem oi
  //   WHERE oi.order.member.id = memberId
  //     AND oi.product.id = productId
  //     AND oi.order.status IN (statuses)
  boolean existsByOrderMemberIdAndProductIdAndOrderStatusIn(
      Long memberId, Long productId, List<OrderStatus> statuses);
}
