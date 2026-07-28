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
import java.time.OffsetDateTime;
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

  // 카드결제/가상계좌 구분. 기존에 저장된 row(카드결제만 있던 시절)에는 값이 없을 수 있어 nullable.
  @Enumerated(EnumType.STRING)
  @Column
  private PaymentMethod paymentMethod;

  @Column(nullable = false)
  private int amount;

  @Column(nullable = false)
  private LocalDateTime paidAt;

  // 토스페이먼츠가 발급한 결제 건 식별자. 기존에 저장된 row에는 값이 없을 수 있어 nullable로 둔다.
  @Column private String paymentKey;

  // 아래 3개는 가상계좌(무통장입금)일 때만 값이 채워진다. 카드결제는 항상 null.
  @Column private String virtualAccountBankCode;
  @Column private String virtualAccountNumber;
  @Column private OffsetDateTime virtualAccountDueDate;

  public Payment(
      Order order,
      PaymentStatus status,
      PaymentMethod paymentMethod,
      int amount,
      String paymentKey) {
    this(order, status, paymentMethod, amount, paymentKey, null, null, null);
  }

  // 가상계좌 발급 시 계좌 정보까지 함께 저장하는 생성자.
  public Payment(
      Order order,
      PaymentStatus status,
      PaymentMethod paymentMethod,
      int amount,
      String paymentKey,
      String virtualAccountBankCode,
      String virtualAccountNumber,
      OffsetDateTime virtualAccountDueDate) {
    this.order = order;
    this.status = status;
    this.paymentMethod = paymentMethod;
    this.amount = amount;
    this.paymentKey = paymentKey;
    this.virtualAccountBankCode = virtualAccountBankCode;
    this.virtualAccountNumber = virtualAccountNumber;
    this.virtualAccountDueDate = virtualAccountDueDate;
    this.paidAt = LocalDateTime.now();
  }

  // 폴링으로 입금이 확인됐을 때 상태만 갱신한다. 계좌 정보는 발급 시점 그대로 남겨 이력이 되게 한다.
  public void completeDeposit() {
    this.status = PaymentStatus.SUCCESS;
  }
}
