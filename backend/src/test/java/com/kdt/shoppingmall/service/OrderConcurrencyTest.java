package com.kdt.shoppingmall.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.kdt.shoppingmall.domain.cart.CartItem;
import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.member.MemberRole;
import com.kdt.shoppingmall.domain.product.Product;
import com.kdt.shoppingmall.dto.order.OrderCreateRequest;
import com.kdt.shoppingmall.exception.InsufficientStockException;
import com.kdt.shoppingmall.repository.CartItemRepository;
import com.kdt.shoppingmall.repository.MemberRepository;
import com.kdt.shoppingmall.repository.ProductRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.TestPropertySource;

// 동시 주문 시 재고 차감이 정확한지 검증하는 "진짜 동시성" 테스트.
//
// 왜 별도 테스트가 필요한가:
//   OrderServiceRetryTest는 Mock이 던진 가짜 OptimisticLockingFailureException으로
//   "예외가 나면 재시도한다"만 확인한다. 반면 이 테스트는 실제 스레드 2개 이상이 같은 product row를
//   동시에 UPDATE하게 만들어, Product의 @Version 컬럼이 정말로 갱신 손실(lost update)을 막는지 확인한다.
//
// @SpringBootTest: 전체 컨텍스트를 띄운다. 실제 DataSource(H2) + @Retryable 프록시 +
//   트랜잭션 매니저가 모두 필요하므로 @DataJpaTest나 Mockito 단위 테스트로는 재현이 불가능하다.
//
// ★ 이 클래스에 @Transactional을 붙이면 안 된다:
//   테스트 메서드에 @Transactional을 붙이면 테스트 스레드가 롤백 전제의 트랜잭션을 잡고 있어
//   워커 스레드가 커밋한 결과를 볼 수 없고, 애초에 락 경쟁도 재현되지 않는다.
//   그래서 롤백에 의존하지 않고, 테스트마다 고유한 상품/회원을 새로 만들어 서로 간섭을 피한다.
@SpringBootTest
@TestPropertySource(
    properties = {
      // 스레드 수(최대 10)보다 커넥션 풀이 작으면 커넥션 대기 때문에 동시성이 재현되지 않는다.
      "spring.datasource.hikari.maximum-pool-size=20",
      // H2는 같은 row를 동시에 UPDATE하면 뒤늦은 쪽을 잠깐 대기시킨다. 기본 타임아웃(1초)이 짧아
      // 락 대기 초과(CannotAcquireLockException)로 흔들릴 수 있으므로 넉넉히 늘린다.
      "spring.datasource.url=jdbc:h2:mem:concurrencydb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000"
    })
class OrderConcurrencyTest {

  @Autowired private OrderService orderService;
  @Autowired private ProductRepository productRepository;
  @Autowired private MemberRepository memberRepository;
  @Autowired private CartItemRepository cartItemRepository;

  // 테스트마다 이메일·상품명이 겹치지 않게 하는 일련번호.
  // (컨텍스트를 공유하고 롤백도 없으므로, 앞선 테스트가 남긴 데이터와 충돌하지 않아야 한다)
  private static final AtomicInteger SEQ = new AtomicInteger();

  @Test
  void 재고와_같은_수의_동시주문은_모두_성공하고_재고가_정확히_0이_된다() throws InterruptedException {
    // given: 재고 2개인 상품 + 각자 1개씩 담아둔 회원 2명
    Long productId = 상품_저장(2);
    List<Long> memberIds = 회원과_장바구니_저장(2, productId, 1);

    // when: 두 스레드가 동시에 주문
    OrderAttemptResult result = 동시_주문_실행(memberIds);

    // then: 둘 다 성공해야 하고, 재고는 정확히 0
    // ── @Version이 없으면 두 트랜잭션이 모두 "재고 2 → 1"로 계산해 UPDATE하므로 최종 재고가 1이 된다.
    //    (한 건의 차감이 사라지는 갱신 손실) 재고가 0이라는 것은 두 차감이 모두 반영됐다는 뜻이다.
    assertThat(result.success()).isEqualTo(2);
    assertThat(잔여_재고(productId)).isZero();
  }

  @Test
  void 재고보다_많은_동시주문은_재고만큼만_성공하고_오버셀이_발생하지_않는다() throws InterruptedException {
    // given: 재고 3개인 상품에 10명이 동시에 1개씩 주문 시도
    int initialStock = 3;
    Long productId = 상품_저장(initialStock);
    List<Long> memberIds = 회원과_장바구니_저장(10, productId, 1);

    // when
    OrderAttemptResult result = 동시_주문_실행(memberIds);
    int remainingStock = 잔여_재고(productId);

    // then: 실행 순서가 매번 달라도 항상 성립하는 "불변식"만 단정한다.
    // ── success()가 정확히 3이라고 단정하지 않는 이유:
    //    @Retryable(maxAttempts=3)이라 10개가 몰리면 정당한 주문도 락 충돌 3회를 소진해
    //    실패할 수 있다. 그 값을 고정 단정하면 CI에서 간헐적으로 깨지는 flaky 테스트가 된다.

    // ① 재고는 절대 음수가 될 수 없다 — 음수면 이미 판 만큼보다 더 팔린 것(오버셀)이다.
    assertThat(remainingStock).as("재고가 음수가 되면 오버셀이 발생한 것이다").isNotNegative();

    // ② 성공 건수가 초기 재고를 넘을 수 없다 — 3개짜리 상품이 4건 이상 팔리면 안 된다.
    assertThat(result.success())
        .as("성공한 주문 수(%d)가 초기 재고(%d)를 넘으면 오버셀이다", result.success(), initialStock)
        .isLessThanOrEqualTo(initialStock);

    // ③ 회계 일치: 줄어든 재고량 = 성공한 주문 수. 실패한 주문이 재고를 갉아먹었거나(롤백 누락),
    //    반대로 성공했는데 차감이 안 된 경우를 잡는다. ①②만으로는 "전원 실패"도 통과하지만
    //    이 등식은 성공 건수와 재고 변화가 1:1로 맞물리는지까지 확인한다.
    assertThat(remainingStock).isEqualTo(initialStock - result.success());
  }

  // ─────────────────────────────── 동시성 실행 하네스 ───────────────────────────────

  // 각 스레드의 결과를 종류별로 센 값. 성공/재고부족/락충돌/그 외로 나눠 담는다.
  private record OrderAttemptResult(
      int success, int outOfStock, int lockConflict, int otherFailure) {}

  // 여러 회원이 "동시에" createOrder를 호출하게 만들고, 결과를 종류별로 집계해서 돌려준다.
  private OrderAttemptResult 동시_주문_실행(List<Long> memberIds) throws InterruptedException {
    int threadCount = memberIds.size();
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);

    // CountDownLatch: 지정한 횟수만큼 countDown()이 불릴 때까지 await()가 대기하는 동기화 장치.
    // 3개를 쓰는 이유 — 이게 없으면 첫 스레드가 주문을 끝낸 뒤 다음 스레드가 시작돼 경쟁이 안 생긴다.
    CountDownLatch ready = new CountDownLatch(threadCount); // ① 전원이 출발선에 섰는지
    CountDownLatch start = new CountDownLatch(1); // ② 동시 출발 신호(0이 되는 순간 전원 해제)
    CountDownLatch done = new CountDownLatch(threadCount); // ③ 전원이 끝났는지

    // AtomicInteger: 여러 스레드가 동시에 ++ 해도 값이 유실되지 않는 카운터(int는 유실될 수 있다).
    AtomicInteger success = new AtomicInteger();
    AtomicInteger outOfStock = new AtomicInteger();
    AtomicInteger lockConflict = new AtomicInteger();
    AtomicInteger otherFailure = new AtomicInteger();

    for (Long memberId : memberIds) {
      executor.submit(
          () -> {
            try {
              ready.countDown(); // 나 준비됐다고 알리고
              start.await(); // 출발 신호를 함께 기다린다
              orderService.createOrder(memberId, 주문요청());
              success.incrementAndGet();
            } catch (InsufficientStockException e) {
              // 재고 소진 후 도착한 요청 — 정상적인 거절
              outOfStock.incrementAndGet();
            } catch (OptimisticLockingFailureException e) {
              // 재시도 3회를 모두 충돌로 소진한 요청 — 오버셀은 아니지만 사용자 경험 손실
              lockConflict.incrementAndGet();
            } catch (Exception e) {
              otherFailure.incrementAndGet();
            } finally {
              done.countDown();
            }
          });
    }

    ready.await(); // 전원이 출발선에 설 때까지 대기
    start.countDown(); // 동시 출발!
    boolean finished = done.await(30, TimeUnit.SECONDS);
    executor.shutdown();
    // 데드락이나 무한 대기로 스레드가 끝나지 않았다면 집계값 자체를 신뢰할 수 없으므로 먼저 확인한다.
    assertThat(finished).as("모든 주문 스레드가 30초 안에 종료돼야 한다").isTrue();

    return new OrderAttemptResult(
        success.get(), outOfStock.get(), lockConflict.get(), otherFailure.get());
  }

  // ─────────────────────────────── 픽스처 준비 ───────────────────────────────

  private OrderCreateRequest 주문요청() {
    // cartItemIds=null → 해당 회원의 장바구니 전체를 주문한다. 배송지는 이 테스트의 관심사가 아니라 null.
    return new OrderCreateRequest(null, "받는이", "010-0000-0000", "12345", "대전시", null, null);
  }

  private Long 상품_저장(int stockQuantity) {
    int seq = SEQ.incrementAndGet();
    return productRepository
        .save(new Product("동시성테스트상품-" + seq, "재고 경쟁 검증용", 10000, stockQuantity, null))
        .getId();
  }

  // 회원 n명을 만들고, 각자 같은 상품을 quantity개씩 장바구니에 담아둔다.
  // 회원마다 장바구니 row가 따로 있어야 "서로 다른 사용자가 같은 상품을 동시에 주문"하는 상황이 된다.
  private List<Long> 회원과_장바구니_저장(int memberCount, Long productId, int quantity) {
    Product product = productRepository.findById(productId).orElseThrow();
    List<Long> memberIds = new ArrayList<>();
    for (int i = 0; i < memberCount; i++) {
      int seq = SEQ.incrementAndGet();
      Member member =
          memberRepository.save(
              new Member(
                  "concurrency" + seq + "@test.com", "encoded", "동시주문자" + seq, MemberRole.USER));
      cartItemRepository.save(new CartItem(member, product, quantity));
      memberIds.add(member.getId());
    }
    return memberIds;
  }

  private int 잔여_재고(Long productId) {
    return productRepository.findById(productId).orElseThrow().getStockQuantity();
  }
}
