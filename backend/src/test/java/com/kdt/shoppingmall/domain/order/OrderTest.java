package com.kdt.shoppingmall.domain.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.member.MemberRole;
import com.kdt.shoppingmall.domain.product.Product;
import org.junit.jupiter.api.Test;

// 순수 도메인 로직(배송비 계산)이므로 Spring 컨테이너나 Mock 없이 일반 JUnit 테스트로 작성한다.
// 이 테스트가 존재하는 이유: 2026-08-02 배포 검증에서 장바구니 화면은 배송비를 포함한 금액을
// 보여주는데 실제 생성된 주문에는 배송비가 전혀 반영되지 않는 결함(DEF-2)이 발견됐다.
// 원인은 배송비 개념 자체가 백엔드에 없었던 것 — applyShippingFee()로 새로 도입한다.
class OrderTest {

  private final Member member = new Member("test@test.com", "encoded", "테스터", MemberRole.USER);

  @Test
  void applyShippingFee_상품금액이_4만원_미만이면_배송비_3500원_부과() {
    Order order = new Order(member);
    Product product = new Product("상품A", "설명", 10000, 100, null);
    order.addItem(new OrderItem(product, product.getPrice(), 2)); // 20,000원

    order.applyShippingFee();

    assertThat(order.getShippingFee()).isEqualTo(3500);
    assertThat(order.getTotalPrice()).isEqualTo(23500);
  }

  @Test
  void applyShippingFee_상품금액이_4만원_이상이면_무료배송() {
    Order order = new Order(member);
    Product product = new Product("상품A", "설명", 10000, 100, null);
    order.addItem(new OrderItem(product, product.getPrice(), 4)); // 40,000원

    order.applyShippingFee();

    assertThat(order.getShippingFee()).isZero();
    assertThat(order.getTotalPrice()).isEqualTo(40000);
  }

  @Test
  void applyShippingFee_경계값_3만9999원은_배송비_부과_4만원은_무료() {
    // 경계값 테스트: 무료배송 기준(40,000원) 바로 아래/정확히 일치하는 두 지점을 함께 검증한다.
    Order below = new Order(member);
    Product cheap = new Product("상품B", "설명", 39999, 100, null);
    below.addItem(new OrderItem(cheap, cheap.getPrice(), 1));
    below.applyShippingFee();
    assertThat(below.getShippingFee()).isEqualTo(3500);

    Order exact = new Order(member);
    Product exactPriced = new Product("상품C", "설명", 40000, 100, null);
    exact.addItem(new OrderItem(exactPriced, exactPriced.getPrice(), 1));
    exact.applyShippingFee();
    assertThat(exact.getShippingFee()).isZero();
  }

  @Test
  void applyShippingFee_여러_상품의_합계_기준으로_판단한다() {
    // 개별 상품이 아니라 장바구니 전체 합계로 무료배송 여부를 판단해야 한다.
    Order order = new Order(member);
    Product a = new Product("상품A", "설명", 15000, 100, null);
    Product b = new Product("상품B", "설명", 30000, 100, null);
    order.addItem(new OrderItem(a, a.getPrice(), 1)); // 15,000원
    order.addItem(new OrderItem(b, b.getPrice(), 1)); // 30,000원, 합계 45,000원

    order.applyShippingFee();

    assertThat(order.getShippingFee()).isZero();
    assertThat(order.getTotalPrice()).isEqualTo(45000);
  }
}
