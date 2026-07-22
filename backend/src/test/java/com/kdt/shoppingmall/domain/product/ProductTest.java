package com.kdt.shoppingmall.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kdt.shoppingmall.exception.InsufficientStockException;
import org.junit.jupiter.api.Test;

// Spring 컨테이너나 Mock 없이도 검증 가능한 순수 도메인 로직이므로,
// @DataJpaTest/@ExtendWith(MockitoExtension.class) 같은 확장 없이 일반 JUnit 테스트로 작성한다.
// (Product.decreaseStock()은 DB나 다른 객체에 의존하지 않는 순수 자바 메서드다)
class ProductTest {

  @Test
  void decreaseStock_재고보다_많은수량_요청시_예외발생() {
    // 재고 5개짜리 상품 준비
    Product product = new Product("상품A", "설명", 10000, 5, null);

    // 재고(5개)보다 많은 10개를 차감하려고 하면 InsufficientStockException이 발생해야 한다.
    assertThatThrownBy(() -> product.decreaseStock(10))
        .isInstanceOf(InsufficientStockException.class)
        .hasMessageContaining("재고가 부족합니다");

    // 예외가 발생했다면 재고는 변경되지 않고 그대로 유지되어야 한다 (중간에 일부만 차감되면 안 됨).
    assertThat(product.getStockQuantity()).isEqualTo(5);
  }

  @Test
  void decreaseStock_재고와_정확히_같은수량_요청시_0개로_감소() {
    // 경계값 테스트: 재고와 요청 수량이 "정확히 같을 때"는 예외가 아니라 정상 처리되어야 한다.
    Product product = new Product("상품A", "설명", 10000, 5, null);

    product.decreaseStock(5);

    assertThat(product.getStockQuantity()).isZero();
  }

  @Test
  void decreaseStock_재고보다_적은수량_요청시_정상차감() {
    Product product = new Product("상품A", "설명", 10000, 10, null);

    product.decreaseStock(3);

    assertThat(product.getStockQuantity()).isEqualTo(7);
  }
}
