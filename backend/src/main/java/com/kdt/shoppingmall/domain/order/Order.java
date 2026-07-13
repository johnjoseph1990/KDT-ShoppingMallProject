package com.kdt.shoppingmall.domain.order;

import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.exception.InvalidOrderStatusException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "member_id", nullable = false)
  private Member member;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderStatus status;

  @Column(nullable = false)
  private int totalPrice;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrderItem> orderItems = new ArrayList<>();

  public Order(Member member) {
    this.member = member;
    this.status = OrderStatus.ORDERED;
  }

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
  }

  public void addItem(OrderItem item) {
    orderItems.add(item);
    item.assignOrder(this);
    this.totalPrice += item.getOrderPrice() * item.getQuantity();
  }

  public void changeStatus(OrderStatus target) {
    if (!this.status.canTransitionTo(target)) {
      throw new InvalidOrderStatusException(
          String.format("주문 상태를 %s에서 %s로 변경할 수 없습니다.", this.status, target));
    }
    this.status = target;
  }
}
