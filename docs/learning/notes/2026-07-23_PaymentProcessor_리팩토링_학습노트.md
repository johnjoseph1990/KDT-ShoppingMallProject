# 2026-07-23 PaymentProcessor 리팩토링 학습노트

> 오늘 한 일: `OrderService.pay()` 안에 박혀있던 `ThreadLocalRandom` 결제 로직을
> `PaymentProcessor` 인터페이스로 추출 → 테스트에서 결제 성공/실패를 결정론적으로 제어 가능하게 만듦.

---

## 1. 리팩토링 배경 — 왜 `@RepeatedTest(5)`로는 부족했나

이전 코드:
```java
// OrderService.java
private static final int MOCK_PAYMENT_SUCCESS_RATE = 90;

boolean success = ThreadLocalRandom.current().nextInt(100) < MOCK_PAYMENT_SUCCESS_RATE;
```

이 코드를 테스트하려면 "성공했을 때"와 "실패했을 때" 두 경로를 모두 검증해야 한다.
그런데 `ThreadLocalRandom`은 Mockito로 Mock할 수 없는 `final` 클래스라 결과를 고정할 방법이 없었다.

그래서 임시로 `@RepeatedTest(5)`를 써서 "5번 돌리면 실패 케이스가 한 번쯤 나오겠지"라고 했는데:
- 90% 성공 확률이라 5번 다 성공하면 재고 복구 경로가 한 번도 검증 안 됨
- CI에서 매번 다른 결과가 나와 테스트를 믿을 수 없음

---

## 2. 해결 방법 — 전략 패턴(Strategy Pattern)으로 분리

**핵심 아이디어**: "결제가 성공인지 실패인지 결정하는 행동"을 인터페이스로 꺼내면,
운영에서는 실제 구현체를, 테스트에서는 Mock을 주입할 수 있다.

```
Before:
OrderService → ThreadLocalRandom (final 클래스, Mock 불가)

After:
OrderService → PaymentProcessor (인터페이스)
                  ↑
    운영: MockPaymentProcessor (ThreadLocalRandom, 90% 성공)
    테스트: Mockito Mock (willReturn(true/false)로 완전 고정)
```

---

## 3. 변경된 파일 구조

```
backend/src/main/java/.../service/
├── PaymentProcessor.java        ← 신규: boolean isSuccess() 인터페이스
├── MockPaymentProcessor.java    ← 신규: @Component, ThreadLocalRandom 90% 구현
└── OrderService.java            ← 수정: 생성자에 PaymentProcessor 추가

backend/src/test/java/.../service/
└── OrderServiceTest.java        ← 수정: @Mock PaymentProcessor 추가,
                                          @RepeatedTest 2개 → @Test 2개로 교체
```

---

## 4. Before / After 코드

### 서비스 (OrderService.java)

```java
// Before: ThreadLocalRandom 직접 사용
private static final int MOCK_PAYMENT_SUCCESS_RATE = 90;
boolean success = ThreadLocalRandom.current().nextInt(100) < MOCK_PAYMENT_SUCCESS_RATE;

// After: 인터페이스 위임
private final PaymentProcessor paymentProcessor;
boolean success = paymentProcessor.isSuccess();
```

### 테스트 (OrderServiceTest.java)

```java
// Before: @RepeatedTest(5) — 비결정론적
@RepeatedTest(5)
void pay_결제결과에따라_재고상태가_올바르다() {
    orderService.pay(1L, 1L);
    if (order.getStatus() == OrderStatus.CANCELED) {  // 이 분기가 안 나올 수도 있음
        assertThat(product.getStockQuantity()).isEqualTo(100);
    }
}

// After: @Test 2개 — 완전 결정론적
@Mock private PaymentProcessor paymentProcessor;

@Test
void pay_결제성공시_PAID_상태_유지() {
    given(paymentProcessor.isSuccess()).willReturn(true);  // 항상 성공
    orderService.pay(1L, 1L);
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);      // 항상 통과
    assertThat(product.getStockQuantity()).isEqualTo(98);           // 항상 통과
}

@Test
void pay_결제실패시_CANCELED_재고복구() {
    given(paymentProcessor.isSuccess()).willReturn(false);  // 항상 실패
    orderService.pay(1L, 1L);
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);  // 항상 통과
    assertThat(product.getStockQuantity()).isEqualTo(100);          // 재고 복구 항상 통과
}
```

---

## 5. 이번 리팩토링에서 배운 개념

### 전략 패턴이란?

"변하는 행동(알고리즘)을 인터페이스로 캡슐화해서 런타임에 교체 가능하게 만드는 패턴"

| 상황 | 주입되는 구현체 |
|---|---|
| 운영(Spring 앱 실행) | `MockPaymentProcessor` (`@Component`로 자동 등록) |
| 단위 테스트 | Mockito `@Mock` (willReturn으로 결과 고정) |
| 향후 실제 PG 연동 | `TossPaymentProcessor` 같은 새 구현체를 추가하면 됨 |

`OrderService`는 `PaymentProcessor` 인터페이스만 알고, 어떤 구현체가 들어오는지 신경 쓰지 않는다.
이걸 **개방-폐쇄 원칙(OCP)**: "확장에는 열려있고, 수정에는 닫혀있다"라고도 표현한다.

### `@InjectMocks`의 타입 매칭

```java
@Mock private PaymentProcessor paymentProcessor;
@InjectMocks private OrderService orderService;
```

Mockito는 `OrderService` 생성자 파라미터를 보고 `PaymentProcessor` 타입을 찾아서 자동 주입한다.
필드 선언 순서를 생성자 파라미터 순서와 맞출 필요가 없다.

단, **같은 타입의 `@Mock`이 두 개면** 필드 이름으로 매칭하므로 이름을 파라미터명과 일치시켜야 한다.

---

## 6. 오늘의 핵심 교훈

**"테스트하기 어려운 코드는 설계가 나쁜 코드다."**

`ThreadLocalRandom`을 서비스 내부에 뒀을 때는 테스트가 불안정했다.
인터페이스로 분리했더니 테스트도 쉬워지고, 나중에 실제 PG 연동도 구현체 하나 추가로 끝난다.
"테스트하기 어렵다"는 신호가 왔을 때 "어떻게 테스트할까"가 아니라 "왜 테스트하기 어려운가"를 먼저 물어야 한다.

---

## 7. 관련 문서

- `docs/learning/topics/spring-test-layers.md` — Section 6: 전략 패턴 + Mock 교체 패턴 추가됨
- `docs/learning/topics/spring-concepts.md` — Section 1: OrderService 생성자 예시 업데이트됨
- 커밋: `2735462` — `refactor: PaymentProcessor 인터페이스로 결제 로직 분리`
