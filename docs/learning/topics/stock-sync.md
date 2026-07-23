# 재고 동기화 설계 — 동시 주문 상황에서도 정확한 재고 처리

> 작성 배경: SPRINT_PLAN.md 요구사항 "동시 주문 상황에서도 정확한 재고 처리가 가능하도록
> 재고 동기화 구조를 설계합니다" 검증 완료 후 학습 문서화 (2026-07-23)

---

## 전체 그림 — 재고가 변하는 경로

```
재고 차감 경로:
  주문 생성(createOrder) → product.decreaseStock(quantity)

재고 복구 경로 (2가지):
  ① 결제 실패(pay() 실패 분기) → OrderService.restoreStock()
  ② 관리자 주문 취소(changeOrderStatus(CANCELED)) → OrderService.restoreStock()

동시성 충돌 경로:
  두 사용자가 같은 상품을 동시에 주문
    → JPA @Version 버전 불일치 감지
    → ObjectOptimisticLockingFailureException 발생
    → GlobalExceptionHandler → HTTP 409 (재시도 메시지)
```

---

## 핵심 개념

### 1. `@Version` — 낙관적 락(Optimistic Locking)

**초보자 설명**
"두 사람이 동시에 같은 상품을 수정하려 할 때, 먼저 저장한 사람만 성공하고
뒤에 저장하려는 사람에게 '이미 바뀐 데이터입니다'라고 알려주는 메커니즘"

**Spring/Java 어느 부분**
JPA 어노테이션. `@Version` 필드가 있는 엔티티는 UPDATE 쿼리에 자동으로
`WHERE version = 현재버전` 조건이 붙는다. 업데이트된 행이 0개면 버전 충돌로 예외를 던진다.

**실무 활용**
재고, 잔액, 좌석 예약처럼 "여러 사람이 동시에 같은 데이터를 바꾸려 하는" 상황에서 사용.

```java
// Product.java
@Version
private Long version;
// → UPDATE product SET stock_quantity=?, version=version+1
//   WHERE id=? AND version=?  ← 이 version 조건이 동시성을 막는다
```

**동작 시나리오 (구체적으로)**

```
1. 사용자 A와 B가 거의 동시에 상품 조회 (둘 다 version=0 상태를 읽음)
2. 사용자 B가 먼저 결제 완료 → DB: version 0→1, 재고 10→7
3. 사용자 A가 뒤늦게 결제 시도 → JPA: UPDATE ... WHERE version=0
   → DB에 version=0인 행이 없음 (이미 1로 바뀜) → 업데이트 행 수 = 0
   → ObjectOptimisticLockingFailureException 발생
4. GlobalExceptionHandler → 409 "다른 주문과 재고 처리가 충돌했습니다. 다시 시도해주세요."
```

---

### 2. 낙관적 락 vs 비관적 락

| 구분 | 낙관적 락 (이 프로젝트) | 비관적 락 |
|---|---|---|
| 방식 | 저장 시점에 버전 비교 | 조회 시점에 DB 행을 잠금 |
| SQL | `WHERE version = ?` 조건 | `SELECT ... FOR UPDATE` |
| 충돌 처리 | 예외 → 클라이언트에 재시도 유도 | 잠금 해제까지 대기 |
| 적합한 상황 | 충돌 빈도가 낮을 때 (일반 쇼핑몰) | 충돌이 잦거나 대기가 허용될 때 |
| 단점 | 충돌이 잦으면 사용자가 계속 재시도해야 함 | 대기 중 다른 요청이 모두 블로킹됨 |

쇼핑몰처럼 "대부분의 주문은 재고가 충분해서 충돌이 드물다"는 전제에서는 낙관적 락이 적합하다.

---

### 3. 재고 차감 — `decreaseStock()`

```java
// Product.java
public void decreaseStock(int quantity) {
    if (this.stockQuantity < quantity) {
        throw new InsufficientStockException(
            "재고가 부족합니다. 상품: " + this.name + ", 재고: " + this.stockQuantity);
    }
    this.stockQuantity -= quantity;
}
```

- 재고 검사와 차감을 **엔티티 안에** 둔 이유: 서비스가 `if (product.getStock() < qty)` 를 직접
  체크하면 그 로직이 여러 서비스에 흩어질 수 있다. 엔티티 안에 있으면 "재고 차감은 항상
  이 규칙을 지킨다"는 걸 구조가 강제한다 — 이를 "도메인 로직을 엔티티에 둔다"고 표현한다.

---

### 4. 재고 복구 — `restoreStock()` (두 경로에서 공유)

```java
// OrderService.java
private void restoreStock(Order order) {
    order.getOrderItems()
         .forEach(item -> item.getProduct().increaseStock(item.getQuantity()));
}

@Transactional
public PaymentResponse pay(Long memberId, Long orderId) {
    ...
    if (!success) {
        order.changeStatus(OrderStatus.CANCELED);
        restoreStock(order);  // ① 결제 실패 자동 취소 경로
    }
    ...
}

@Transactional
public OrderResponse changeOrderStatus(Long orderId, OrderStatus status) {
    ...
    if (status == OrderStatus.CANCELED) {
        restoreStock(order);  // ② 관리자 직접 취소 경로
    }
    ...
}
```

처음에 버그가 있었다: ②번 경로(관리자 취소)에 재고 복구가 없었다. 발견 계기는
`2026-07-23_인증주문요구사항검증_학습노트.md`의 Section 2 참고.

---

## 테스트 전략 — 3계층 완전 검증

동시 주문 재고 처리는 레이어마다 다른 종류의 테스트가 필요하다.

### 계층 1: `@DataJpaTest` — 낙관적 락 동작 자체 검증

```java
// ProductRepositoryTest.java
@Test
void 동시에_같은_상품을_조회한_두_거래_중_먼저_커밋한_쪽만_성공한다() {
    Product userA = productRepository.findById(productId).orElseThrow(); // version=0
    em.clear();
    Product userB = productRepository.findById(productId).orElseThrow(); // version=0

    userB.decreaseStock(3);
    productRepository.saveAndFlush(userB);  // version 0→1, 재고 10→7

    userA.decreaseStock(5);
    assertThatThrownBy(() -> productRepository.saveAndFlush(userA))
        .isInstanceOf(ObjectOptimisticLockingFailureException.class);  // version 불일치!

    em.clear();
    assertThat(productRepository.findById(productId).orElseThrow().getStockQuantity())
        .isEqualTo(7);  // B만 반영, A는 롤백
}
```

**왜 이 계층이 필요한가**: Mock으로는 낙관적 락을 검증할 수 없다. 낙관적 락 충돌은
"진짜 flush/commit 시점에 DB가 version을 비교"하기 때문에, 가짜 Repository로는 이 동작이 일어나지 않는다.

**왜 실제 멀티스레드 테스트는 안 쓰는가**:
- H2 인메모리 DB는 PostgreSQL의 실제 row-level lock 동작과 다를 수 있어 결과가 환경마다 다름
- `EntityManager.clear()`로 "다른 세션에서 읽은 상태"를 충분히 흉내낼 수 있음
- 멀티스레드 테스트는 타이밍에 따라 간헐적으로 실패하는 "flaky test"가 되기 쉬움

### 계층 2: `@ExtendWith(MockitoExtension)` — 재고 처리 비즈니스 로직 검증

```java
// OrderServiceTest.java

// 재고 부족 시 InsufficientStockException이 서비스 밖으로 전파되는지
@Test
void createOrder_재고부족_예외발생() { ... }

// 결제 성공 → 재고가 차감된 상태(98) 유지
@Test
void pay_결제성공시_PAID_상태_유지() { ... }

// 결제 실패 → 재고가 100으로 복구
@Test
void pay_결제실패시_CANCELED_재고복구() { ... }

// 관리자 취소 → 재고 복구
@Test
void changeOrderStatus_취소시_재고복구() { ... }
```

### 계층 3: `@WebMvcTest` — 예외 → HTTP 응답 변환 검증

```java
// OrderControllerTest.java

// InsufficientStockException → 409 CONFLICT
@Test
void 주문생성_재고부족_409() { ... }

// OptimisticLockingFailureException → 409 CONFLICT + 재시도 안내 메시지
@Test
void 주문생성_동시성충돌_409() { ... }
```

### 왜 세 계층이 모두 필요한가

| 계층 | 검증하는 것 | 검증 못 하는 것 |
|---|---|---|
| `@DataJpaTest` | JPA가 실제로 version을 올리고 충돌을 감지하는가 | HTTP 응답 코드, 비즈니스 흐름 |
| `Mockito` 서비스 | 재고 차감/복구 흐름이 맞는가 | DB 동작, HTTP 변환 |
| `@WebMvcTest` 컨트롤러 | 예외가 올바른 HTTP 코드(409)로 응답하는가 | 실제 DB 동작, 비즈니스 흐름 |

세 계층이 서로의 빈 곳을 메워서 전체가 완전한 검증이 된다.

---

## 파일 구조 (관련 파일 전체)

```
backend/src/main/java/
├── domain/product/
│   └── Product.java                   ← @Version, decreaseStock(), increaseStock()
├── exception/
│   ├── InsufficientStockException.java ← RuntimeException 확장, 재고 부족 전용
│   └── GlobalExceptionHandler.java    ← InsufficientStockException → 409
│                                         OptimisticLockingFailureException → 409
└── service/
    └── OrderService.java              ← createOrder(차감), restoreStock(복구 공통)

backend/src/test/java/
├── repository/
│   └── ProductRepositoryTest.java     ← @DataJpaTest: 낙관적 락 동작 자체 검증
├── service/
│   └── OrderServiceTest.java          ← 재고 차감/복구 비즈니스 로직 검증
└── controller/
    └── OrderControllerTest.java       ← 예외 → HTTP 409 변환 검증
```

---

## 자주 하는 실수

1. **재고 복구 경로 누락**: 취소 경로가 여러 개면 하나를 빠뜨리기 쉽다. 이 프로젝트에서도
   관리자 직접 취소 경로에 복구 로직이 없었던 버그가 있었다 → `restoreStock()` 공통 메서드로
   해결하고, 두 경로 모두에서 호출하는 방식으로 구조를 통일했다.

2. **낙관적 락을 Mock 테스트로 검증하려는 시도**: Mock은 버전 비교를 하지 않아서 항상 성공한다.
   낙관적 락 동작은 반드시 `@DataJpaTest`(실제 DB)로 검증해야 한다.

3. **`em.clear()` 생략**: `@DataJpaTest`에서 같은 `EntityManager`로 두 번 `findById()`를 하면
   1차 캐시에서 같은 객체를 반환한다 — "두 사용자가 독립적으로 같은 상품을 읽은" 상황을
   흉내낼 수 없다. `clear()` 호출 후 다시 조회해야 별개의 스냅샷이 만들어진다.

---

## 셀프 체크 (답을 보지 말고 먼저 떠올려 볼 것)

> 이 문서를 다시 열 때마다, 본문을 읽기 전에 아래 질문에 먼저 답해보세요.

1. `Product`에 `@Version`을 붙이면 JPA가 UPDATE 쿼리를 어떻게 바꾸는가? 충돌은 어떤 조건에서 감지되는가?
2. 낙관적 락과 비관적 락의 차이는? 쇼핑몰에서 낙관적 락을 선택한 이유는?
3. 재고 복구(`restoreStock`)가 필요한 경로가 두 가지인 이유는? 하나만 있으면 어떤 버그가 생기는가?
4. `@DataJpaTest`에서 낙관적 락을 테스트할 때 `em.clear()`를 두 조회 사이에 넣는 이유는?
5. Mockito 서비스 테스트로는 낙관적 락 동작을 검증할 수 없는 이유는? 어느 계층의 테스트로 대신 검증하는가?
