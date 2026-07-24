package com.kdt.shoppingmall.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kdt.shoppingmall.domain.product.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

// @DataJpaTest: 실제 H2 DB로 Product의 @Version(낙관적 락)이 진짜로 동작하는지 검증한다.
// Mockito로 Repository를 가짜로 만드는 서비스 테스트로는 이 시나리오를 검증할 수 없다 —
// 낙관적 락 충돌은 "진짜 flush/commit이 일어날 때" DB가 버전을 비교하면서 감지하는 것이라,
// Mock 객체는 애초에 버전 비교라는 개념 자체가 없기 때문이다.
@DataJpaTest
class ProductRepositoryTest {

  @Autowired private TestEntityManager em;

  @Autowired private ProductRepository productRepository;

  private Long productId;

  @BeforeEach
  void setUp() {
    Product product = em.persistAndFlush(new Product("상품A", "설명", 10000, 10, null));
    productId = product.getId();
    // 영속성 컨텍스트를 비워서, 이후의 조회가 "새로 시작하는 세션"처럼 동작하게 만든다.
    // 비우지 않으면 findById()가 1차 캐시에 있는 같은 객체를 반환해서 두 사용자를 흉내낼 수 없다.
    em.clear();
  }

  @Test
  void 동시에_같은_상품을_조회한_두_거래_중_먼저_커밋한_쪽만_성공한다() {
    // 두 사용자가 거의 동시에 같은 상품 페이지를 봤다고 가정한다.
    // 둘 다 아직 아무도 재고를 차감하지 않은 시점(version=0)의 상품을 읽는다.
    Product userA = productRepository.findById(productId).orElseThrow();
    em.clear(); // userA를 준영속 상태로 만들어, 다음 조회가 별도의 스냅샷을 갖도록 한다.
    Product userB = productRepository.findById(productId).orElseThrow();

    // 사용자 B가 먼저 결제를 완료해서 재고 3개를 차감하고 커밋한다 (version: 0 → 1).
    userB.decreaseStock(3);
    productRepository.saveAndFlush(userB);

    // 사용자 A는 자신이 읽었던 시점(version=0)을 기준으로 재고 5개 차감을 시도한다.
    // DB의 버전은 이미 1이 되었으므로, 저장 시점에 낙관적 락 충돌이 감지되어야 한다.
    userA.decreaseStock(5);
    assertThatThrownBy(() -> productRepository.saveAndFlush(userA))
        .isInstanceOf(ObjectOptimisticLockingFailureException.class);

    // 최종 재고는 먼저 커밋에 성공한 사용자 B의 차감(3개)만 반영되어야 한다 (10 - 3 = 7).
    // 사용자 A의 차감은 커밋되지 못했으므로 재고에 반영되면 안 된다 (이중 차감/초과 판매 방지).
    em.clear();
    Product result = productRepository.findById(productId).orElseThrow();
    assertThat(result.getStockQuantity()).isEqualTo(7);
  }

  // 시드 데이터의 멱등성(중복 방지)을 이름 기준으로 판단하기 위해 existsByName을 검증한다.
  @Test
  void 이름으로_상품_존재_여부를_확인한다() {
    // setUp에서 "상품A"를 저장했으므로 true, 없는 이름은 false 여야 한다.
    assertThat(productRepository.existsByName("상품A")).isTrue();
    assertThat(productRepository.existsByName("존재하지_않는_상품")).isFalse();
  }
}
