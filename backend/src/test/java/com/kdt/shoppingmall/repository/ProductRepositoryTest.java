package com.kdt.shoppingmall.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.member.MemberRole;
import com.kdt.shoppingmall.domain.order.Order;
import com.kdt.shoppingmall.domain.order.OrderItem;
import com.kdt.shoppingmall.domain.order.OrderStatus;
import com.kdt.shoppingmall.domain.product.Product;
import com.kdt.shoppingmall.domain.review.Review;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

// @DataJpaTest: 실제 H2 DB로 Product의 @Version(낙관적 락)이 진짜로 동작하는지 검증한다.
// Mockito로 Repository를 가짜로 만드는 서비스 테스트로는 이 시나리오를 검증할 수 없다 —
// 낙관적 락 충돌은 "진짜 flush/commit이 일어날 때" DB가 버전을 비교하면서 감지하는 것이라,
// Mock 객체는 애초에 버전 비교라는 개념 자체가 없기 때문이다.
@DataJpaTest
class ProductRepositoryTest {

  @Autowired private TestEntityManager em;
  @Autowired private ProductRepository productRepository;
  @Autowired private ReviewRepository reviewRepository;
  @Autowired private MemberRepository memberRepository;
  @Autowired private OrderRepository orderRepository;

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

  // ── [P1-2] findBestProductsWithAvgRating 검증 ──────────────────────────────

  // 리뷰가 정확히 5개(경계값)인 상품은 베스트 후보에 포함되어야 한다 (>= 5 는 포함).
  @Test
  void findBestProductsWithAvgRating_리뷰5개이상인_상품만_반환한다() {
    Product best = productRepository.findById(productId).orElseThrow(); // setUp의 상품A
    Product notEnough = em.persistAndFlush(new Product("상품B", "설명", 5000, 5, null));
    em.clear();

    // 상품A: 리뷰 5개 (컷오프 통과)
    saveReviews(best, 5, 4); // 5명의 회원이 별점 4점씩
    // 상품B: 리뷰 4개 (컷오프 미달)
    saveReviews(notEnough, 4, 5);
    em.flush();
    em.clear();

    Page<Object[]> result = productRepository.findBestProductsWithAvgRating(PageRequest.of(0, 10));

    assertThat(result.getTotalElements()).isEqualTo(1);
    Product returned = (Product) result.getContent().get(0)[0];
    assertThat(returned.getName()).isEqualTo("상품A");
  }

  // 두 상품 모두 5개 이상일 때 평균 별점 내림차순으로 정렬한다.
  @Test
  void findBestProductsWithAvgRating_평균별점_내림차순_정렬한다() {
    Product high = productRepository.findById(productId).orElseThrow(); // 상품A
    Product low = em.persistAndFlush(new Product("상품B", "설명", 5000, 5, null));
    em.clear();

    saveReviews(high, 5, 5); // 평균 5.0
    saveReviews(low, 5, 2); // 평균 2.0
    em.flush();
    em.clear();

    Page<Object[]> result = productRepository.findBestProductsWithAvgRating(PageRequest.of(0, 10));

    assertThat(result.getTotalElements()).isEqualTo(2);
    Product first = (Product) result.getContent().get(0)[0];
    assertThat(first.getName()).isEqualTo("상품A"); // 높은 평점이 먼저
  }

  // 리뷰가 전혀 없을 때 빈 페이지를 반환한다 (폴백 전환 트리거 조건).
  @Test
  void findBestProductsWithAvgRating_리뷰없으면_빈페이지반환() {
    // setUp의 상품A에 리뷰 없음
    Page<Object[]> result = productRepository.findBestProductsWithAvgRating(Pageable.unpaged());

    assertThat(result.isEmpty()).isTrue();
  }

  // ── [P1-3] findTopBySales 검증 ──────────────────────────────────────────────

  // PAID 이상 주문에서 판매 수량이 많은 상품이 먼저 반환된다.
  @Test
  void findTopBySales_판매량_내림차순_정렬한다() {
    Product high = productRepository.findById(productId).orElseThrow(); // 상품A
    Product low = em.persistAndFlush(new Product("상품B", "설명", 5000, 5, null));
    em.clear();

    Member buyer = saveMember("buyer@test.com");

    // 상품A: 3번 주문 (PAID)
    saveOrder(buyer, high, 3);
    // 상품B: 1번 주문 (PAID)
    saveOrder(buyer, low, 1);
    em.flush();
    em.clear();

    List<OrderStatus> statuses =
        List.of(OrderStatus.PAID, OrderStatus.SHIPPING, OrderStatus.DELIVERED);
    Page<Object[]> result = productRepository.findTopBySales(statuses, PageRequest.of(0, 10));

    assertThat(result.getTotalElements()).isEqualTo(2);
    Product first = (Product) result.getContent().get(0)[0];
    assertThat(first.getName()).isEqualTo("상품A");
  }

  // ORDERED(결제 전) 상태의 주문은 판매량에 집계되지 않는다.
  @Test
  void findTopBySales_ORDERED상태는_집계_제외() {
    Product product = productRepository.findById(productId).orElseThrow();
    em.clear();

    Member buyer = saveMember("buyer2@test.com");
    // ORDERED(결제 전) 상태로만 주문
    Order order = orderRepository.save(new Order(buyer));
    order.addItem(new OrderItem(product, product.getPrice(), 1));
    orderRepository.saveAndFlush(order);
    em.clear();

    List<OrderStatus> statuses =
        List.of(OrderStatus.PAID, OrderStatus.SHIPPING, OrderStatus.DELIVERED);
    Page<Object[]> result = productRepository.findTopBySales(statuses, PageRequest.of(0, 10));

    assertThat(result.isEmpty()).isTrue();
  }

  // ── [viewCount] findByIdNotOrderByViewCountDesc 검증 ─────────────────────────

  // 조회수 높은 상품이 먼저 반환되어야 한다 (추천 폴백 2순위).
  @Test
  void findByIdNotOrderByViewCountDesc_조회수_내림차순_정렬한다() {
    Product highView = productRepository.findById(productId).orElseThrow();
    Product lowView = em.persistAndFlush(new Product("상품B", "설명", 5000, 5, null));
    em.clear();

    // 상품A: 조회수 5회 누적 — increaseViewCount()를 5번 호출 후 flush
    highView = productRepository.findById(productId).orElseThrow();
    for (int i = 0; i < 5; i++) highView.increaseViewCount();
    productRepository.saveAndFlush(highView);
    em.clear();

    Page<Product> result =
        productRepository.findByIdNotOrderByViewCountDesc(-1L, PageRequest.of(0, 10));

    assertThat(result.getTotalElements()).isEqualTo(2);
    assertThat(result.getContent().get(0).getName()).isEqualTo("상품A"); // 조회수 5 → 먼저
    assertThat(result.getContent().get(1).getName()).isEqualTo("상품B"); // 조회수 0 → 나중
    assertThat(result.getContent().get(0).getViewCount()).isEqualTo(5);
  }

  // ── 헬퍼 메서드 ──────────────────────────────────────────────────────────────

  // 동일 상품에 리뷰는 회원 1명당 1개만 가능(유니크 제약)하므로, 회원을 count명 생성한다.
  private void saveReviews(Product product, int count, int rating) {
    for (int i = 0; i < count; i++) {
      // prefix로 상품ID를 써서 서로 다른 테스트 간 이메일 충돌을 방지한다.
      String email = "rev_" + product.getId() + "_" + i + "@test.com";
      Member m = memberRepository.save(new Member(email, "pw", "이름" + i, MemberRole.USER));
      reviewRepository.save(new Review(m, product, rating, "리뷰"));
    }
  }

  private Member saveMember(String email) {
    return memberRepository.save(new Member(email, "pw", "구매자", MemberRole.USER));
  }

  private void saveOrder(Member buyer, Product product, int quantity) {
    Order order = orderRepository.save(new Order(buyer));
    order.addItem(new OrderItem(product, product.getPrice(), quantity));
    order.changeStatus(OrderStatus.PAID);
    orderRepository.save(order);
  }
}
