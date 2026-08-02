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

  // 배송비 정책. 주문 시점의 상품 합계로 판단해 Order에 스냅샷으로 저장한다 — OrderItem의
  // orderPrice와 같은 이유로, 나중에 정책(기준 금액·배송비)이 바뀌어도 이미 만든 주문의
  // 금액은 그대로 유지되어야 하기 때문이다. 프론트(CartPage.jsx의 FREE_SHIP/3500)에도
  // 같은 값이 미리보기용으로 있으니, 한쪽을 바꾸면 반드시 다른 쪽도 확인한다.
  public static final int FREE_SHIPPING_THRESHOLD = 40000;
  public static final int SHIPPING_FEE = 3500;

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

  // columnDefinition으로 기본값을 명시해야 한다 — 기본값 없이 nullable=false만 붙이면
  // 기존 row가 있는 orders 테이블에 ddl-auto=update가 이 컬럼을 추가하는 DDL 자체가
  // 거부된다(과거 Product.viewCount 필드에서 겪었던 것과 같은 함정).
  @Column(nullable = false, columnDefinition = "integer default 0")
  private int shippingFee;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrderItem> orderItems = new ArrayList<>();

  // 배송지 정보 — 주문 시점의 주소를 저장해 이후 회원이 주소를 바꿔도 변하지 않게 한다.
  @Column private String deliveryName;

  @Column private String deliveryPhone;

  @Column private String deliveryZipCode;

  @Column private String deliveryAddress;

  @Column private String deliveryAddressDetail;

  @Column private String deliveryNote;

  // 테스트 등 배송지 없이 Order를 만들어야 할 때 사용하는 생성자
  public Order(Member member) {
    this.member = member;
    this.status = OrderStatus.ORDERED;
  }

  // 실제 주문 생성 시 배송지 정보를 함께 저장하는 생성자
  public Order(
      Member member,
      String deliveryName,
      String deliveryPhone,
      String deliveryZipCode,
      String deliveryAddress,
      String deliveryAddressDetail,
      String deliveryNote) {
    this.member = member;
    this.status = OrderStatus.ORDERED;
    this.deliveryName = deliveryName;
    this.deliveryPhone = deliveryPhone;
    this.deliveryZipCode = deliveryZipCode;
    this.deliveryAddress = deliveryAddress;
    this.deliveryAddressDetail = deliveryAddressDetail;
    this.deliveryNote = deliveryNote;
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

  // 상품을 모두 담은 뒤(addItem 반복 종료 후) 한 번만 호출한다. 배송비는 "장바구니 전체
  // 합계"를 기준으로 판단해야 하므로, 상품을 하나씩 담는 addItem 안에서는 계산할 수 없다.
  public void applyShippingFee() {
    this.shippingFee = this.totalPrice >= FREE_SHIPPING_THRESHOLD ? 0 : SHIPPING_FEE;
    this.totalPrice += this.shippingFee;
  }

  public void changeStatus(OrderStatus target) {
    if (!this.status.canTransitionTo(target)) {
      throw new InvalidOrderStatusException(
          String.format("주문 상태를 %s에서 %s로 변경할 수 없습니다.", this.status, target));
    }
    this.status = target;
  }
}
