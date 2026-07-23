package com.kdt.shoppingmall.domain.payment;

import com.kdt.shoppingmall.domain.order.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne
  @JoinColumn(name = "orders_id", nullable = false, unique = true)
  private Order order;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentStatus status;

  @Column(nullable = false)
  private int amount;

  @Column(nullable = false)
  private LocalDateTime paidAt;

  // 토스페이먼츠가 발급한 결제 건 식별자. 기존에 저장된 row에는 값이 없을 수 있어 nullable로 둔다.
  @Column private String paymentKey;

  public Payment(Order order, PaymentStatus status, int amount, String paymentKey) {
    this.order = order;
    this.status = status;
    this.amount = amount;
    this.paymentKey = paymentKey;
    this.paidAt = LocalDateTime.now();
  }
}
