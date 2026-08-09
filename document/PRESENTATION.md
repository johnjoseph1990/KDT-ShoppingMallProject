# MINS Farmers Market — 발표자료

> **발표 분량:** 15분 + Q&A
> **원본(캐노니컬):** 이 파일. HTML 슬라이드(`document/presentation.html`, git 미추적)는 이 내용을 옮긴 것이므로,
> 내용 수정은 **여기서 먼저** 한다. HTML은 화면에 맞추느라 부록 A를 두 장으로 나눠 총 32장이다(본문 29 + 부록 3).
> **수치 기준일:** 2026-08-08 (실제 `./gradlew test` · `npm run test:run` 실행 결과, CI `31241205478`)
> **주의:** 이 수치는 `FINAL_DELIVERABLE.md` §5와 **반드시 같아야 한다** — 한 zip 안의 두 PDF가
> 서로 다른 숫자를 말하면 심사자는 어느 쪽도 믿지 않는다. 수치를 고칠 때는 양쪽을 함께 고친다.

---

## 1. 표지

# MINS Farmers Market

### 지역 소농 직거래 쇼핑몰

최민영 · KDT 풀스택 개인 프로젝트

🔗 https://www.minsdev.works

---

## 2. 한 줄 소개

> **대전·충남 지역 소농이 직접 수확한 농산물을, 중간 유통 없이 파는 쇼핑몰.**

기술적으로는 —
**회원 · 상품 · 장바구니 · 주문 · 결제 · 관리자 · 리뷰**까지
쇼핑몰의 전체 도메인을 직접 설계하고 구현한 프로젝트입니다.

말하기: "쇼핑몰은 흔한 주제입니다. 그래서 저는 '기능을 몇 개 만들었나'가 아니라
**'돈과 재고가 걸린 지점에서 데이터가 깨지지 않게 어떻게 막았나'**를 중심으로 말씀드리겠습니다."

---

## 3. 왜 쇼핑몰이었나

| 배우고 싶었던 것 | 쇼핑몰에서 자연스럽게 나오는 문제 |
|---|---|
| 도메인 설계 | 회원-주문-상품-결제가 얽힌 관계를 테이블로 옮기기 |
| 트랜잭션·동시성 | 재고 1개에 주문 2건이 동시에 들어오면? |
| 상태 관리 | 주문이 "결제 전 → 배송 완료"로 건너뛰면 안 됨 |
| 외부 연동 | 실제 PG사(토스페이먼츠)와 통신 |
| 인가 | 같은 API를 관리자와 일반 회원이 다르게 봐야 함 |

**CRUD만으로는 만날 수 없는 문제들이 전부 들어 있는 주제**라서 골랐습니다.

---

## 4. 시스템 구성

```
┌─────────────┐      ┌──────────────────┐      ┌────────────────────┐
│  React 18   │─────▶│  Spring Boot 3   │─────▶│  PostgreSQL        │
│  (Vite)     │ REST │  Spring Security │ JPA  │  (Flexible Server) │
│  Custom CSS │◀─────│  Spring Data JPA │◀─────│                    │
└─────────────┘      └────────┬─────────┘      └────────────────────┘
                              │
                     ┌────────┴────────┐
                     ▼                 ▼
            ┌────────────────┐  ┌──────────────┐
            │ 토스페이먼츠     │  │ Blob Storage │
            │ (카드·가상계좌)  │  │ (상품 이미지) │
            └────────────────┘  └──────────────┘

                    ⬆ 전부 Azure Container Apps 위에서 동작
```

**전체가 `azure/setup.sh` 한 파일로 재생성 가능합니다.** (뒤에서 다시 설명)

---

## 5. 라이브 데모

### 👉 https://www.minsdev.works

**시연 순서 (5분)**

1. 홈 → 이번 주 수확물
2. 상품 목록 → 태그 · 별점 필터
3. 상품 상세 → 리뷰 · 키워드 배지
4. 장바구니 → 주문서 작성
5. 토스페이먼츠 결제창 (카드 / 가상계좌)
6. 마이페이지 → 주문 내역 → 리뷰 작성
7. 관리자 로그인 → 상품 등록 · 주문 상태 변경

> 데모 계정은 `DevDataInitializer`가 시드합니다 — `admin@`(관리자) / `test@`(일반)

---

## 6. 기능 한눈에

| 도메인 | 구현한 기능 |
|---|---|
| **회원** | 회원가입 · 로그인/로그아웃 · 내 정보 수정 · 탈퇴 · 권한 분리(USER/ADMIN) |
| **상품** | 등록 · 조회 · 수정 · 삭제 · 이미지 업로드 · 태그 |
| **검색/추천** | 키워드 검색 · 태그 필터 · 최소 별점 필터 · 베스트 상품 · 연관 추천 |
| **장바구니** | 담기 · 수량 변경 · 삭제 |
| **주문** | 주문 생성 · 내역 조회 · 상세 조회 · 배송지 스냅샷 |
| **결제** | 토스페이먼츠 카드 결제 · 가상계좌 발급 · 입금 확인(폴링) |
| **관리자** | 상품 CRUD · 전체 주문 조회 · 상태 필터 · 주문 상태 변경 |
| **리뷰** | 작성 · 조회 · 수정 · 삭제 · 별점 평균 집계 · 키워드 자동 추출 |

---

## 7. 데이터 모델 (ERD)

| 테이블 | 역할 | 관계 |
|---|---|---|
| `member` | 회원 (일반/관리자) | 주문·장바구니·리뷰의 주체 |
| `product` | 상품 (재고 · 이미지 · **낙관적 락 version**) | 모든 거래의 대상 |
| `product_tag` | 상품 태그 | product 1:N |
| `cart_item` | 장바구니 항목 | member 1:N, product 1:N |
| `orders` | 주문 (**상태 흐름 관리**) | member 1:N |
| `order_item` | 주문 상세 (수량 · **당시 가격**) | orders 1:N, product 1:N |
| `payment` | 결제 기록 | orders 1:1 |
| `review` | 리뷰 (별점 1~5 + 내용) | member 1:N, product 1:N |

---

## 8. 설계 결정 ① — 왜 `order_item`에 가격을 복사해 두는가

**문제:** 주문 후에 관리자가 상품 가격을 바꾸면?

```
❌ order_item이 product를 참조만 하면
   → 3개월 전 주문 내역을 열었을 때 "지금 가격"이 보인다
   → 실제로 결제한 금액과 다름 = 회계상 사고
```

**해결: 주문 시점의 가격을 `order_item.order_price`에 복사(스냅샷)한다.**

```java
order.addItem(new OrderItem(product, product.getPrice(), cartItem.getQuantity()));
//                                    ^^^^^^^^^^^^^^^^^ 지금 이 순간의 가격을 박아둔다
```

같은 이유로 **배송지도 주문에 복사**합니다 — 나중에 회원이 주소를 바꿔도 과거 주문서는 그대로여야 하니까요.

> 💡 정규화가 항상 정답은 아니다. **"시간이 지나도 변하면 안 되는 값"은 의도적으로 중복 저장**한다.

---

## 9. 설계 결정 ② — 왜 `payment`가 별도 테이블인가

`orders`에 `payment_key` 컬럼 하나 추가하면 될 것 같지만 —

- 결제는 **실패하고 재시도**될 수 있다 (주문 1건에 결제 시도 N건)
- 결제 수단마다 **필요한 필드가 다르다** (가상계좌는 은행코드·계좌번호·입금기한이 추가로 필요)
- 결제 정보는 **감사(audit) 대상**이라 주문과 생명주기가 다르다

→ 주문 테이블을 결제 수단이 늘어날 때마다 넓히지 않기 위해 분리했습니다.

---

## 10. ⚡ 기술 하이라이트 ① — 동시 주문과 재고

### 문제: 재고가 1개인데 두 사람이 동시에 주문하면?

```
시각   주문 A                    주문 B
───────────────────────────────────────────────
t1     재고 읽기 → 1
t2                              재고 읽기 → 1
t3     1 - 1 = 0 저장
t4                              1 - 1 = 0 저장   ← A의 차감이 사라짐

결과: 재고 0인데 주문은 2건 = 재고보다 많이 팔림 (oversell)
```

이게 **레이스 컨디션**입니다. 각 요청은 혼자서는 완벽히 정상이라 테스트로도 잘 안 잡힙니다.

---

## 11. ⚡ 해결 ① — 낙관적 락 + 자동 재시도

**1단계. 상품에 버전을 붙인다** (`Product.java`)

```java
// @Version: JPA가 UPDATE할 때 "내가 읽은 버전이 그대로인가"를 함께 검사한다.
// 그 사이 다른 트랜잭션이 값을 바꿨으면 버전이 안 맞아 예외가 터진다.
@Version private Long version;
```

실제로 나가는 SQL:
```sql
UPDATE product SET stock_quantity = 0, version = 6
WHERE id = 3 AND version = 5;   -- ← 0건 업데이트되면 충돌!
```

**2단계. 충돌하면 다시 시도한다** (`OrderService.java`)

```java
@Retryable(
    retryFor = OptimisticLockingFailureException.class,
    maxAttempts = 3,                      // 최대 3번
    backoff = @Backoff(delay = 50))       // 50ms 쉬었다가 재시도
@Transactional
public OrderResponse createOrder(Long memberId, OrderCreateRequest request) { ... }
```

---

## 12. ⚡ 해결 ① — 여기서 한 번 틀렸던 것

**어노테이션 순서가 중요합니다.**

```java
@Retryable      // ← 바깥
@Transactional  // ← 안쪽
```

`@Retryable`이 **바깥**에 있어야 재시도마다 **새 트랜잭션**이 열립니다.

만약 반대였다면? 롤백된 트랜잭션 안에서 재시도하게 되어
**이미 죽은 트랜잭션을 계속 재사용** → 3번 다 실패합니다.

> 스프링 AOP에서 여러 어노테이션이 붙으면 **어드바이스에 순서가 있다**는 걸
> 이때 처음 알았습니다. 코드에 주석으로 남겨뒀습니다.

3번 모두 실패하면 → `GlobalExceptionHandler`가 **409 Conflict**로 응답합니다.

---

## 13. ⚡ 해결 ① — 왜 비관적 락이 아닌가

| | 비관적 락 (`SELECT FOR UPDATE`) | **낙관적 락 (선택)** |
|---|---|---|
| 방식 | 먼저 잡고 남은 사람은 **기다리게** 함 | 일단 진행, **충돌하면** 되돌림 |
| 충돌이 드물 때 | 매번 락 비용을 냄 (손해) | ✅ 거의 공짜 |
| 충돌이 잦을 때 | ✅ 유리 | 재시도가 늘어남 |
| 데드락 | 발생 가능 | 없음 |

**판단:** 일반 쇼핑몰은 같은 상품에 동시 주문이 몰리는 일이 드뭅니다.
(단, 한정판 티켓팅처럼 충돌이 확실하다면 비관적 락이 맞습니다.)

> 💡 "무엇을 썼나"보다 **"왜 그걸 골랐나"**가 설계입니다.

---

## 14. ⚡ 기술 하이라이트 ② — 주문 상태가 아무렇게나 바뀌면?

**문제:** 결제도 안 한 주문이 "배송 완료"가 될 수 있으면 안 됩니다.

`if (status == ...)` 조건문을 서비스 곳곳에 흩뿌리면 —
새 상태 하나를 추가할 때마다 **고쳐야 할 곳을 찾아다녀야** 합니다.

**해결: 전이 규칙을 enum 자신이 들고 있게 한다.** (상태 머신)

```java
public enum OrderStatus {
  ORDERED, WAITING_FOR_DEPOSIT, PAID, SHIPPING, DELIVERED, CANCELED;

  public boolean canTransitionTo(OrderStatus target) {
    return switch (this) {
      case ORDERED             -> target == PAID || target == CANCELED
                                  || target == WAITING_FOR_DEPOSIT;
      case WAITING_FOR_DEPOSIT -> target == PAID || target == CANCELED;
      case PAID                -> target == SHIPPING || target == CANCELED;
      case SHIPPING            -> target == DELIVERED;
      default                  -> false;   // DELIVERED · CANCELED는 종착점
    };
  }
}
```

---

## 15. ⚡ 상태 전이도

```
        ORDERED ─── 카드결제 ────────────────▶ PAID ──▶ SHIPPING ──▶ DELIVERED (종착)
           │                                    ▲
           ├──── 가상계좌 ──▶ WAITING_FOR_DEPOSIT ┘  (입금 확인)
           │                        ╎
           ╰────────────────────────┴──────────╌╌▶ CANCELED (종착)
                        (취소는 ORDERED · WAITING_FOR_DEPOSIT · PAID 에서만)
```

| 현재 상태 | 갈 수 있는 곳 |
|---|---|
| `ORDERED` | `PAID` · `WAITING_FOR_DEPOSIT` · `CANCELED` |
| `WAITING_FOR_DEPOSIT` | `PAID` · `CANCELED` |
| `PAID` | `SHIPPING` · `CANCELED` |
| `SHIPPING` | `DELIVERED` **만** — 배송이 시작되면 취소할 수 없다 |
| `DELIVERED` · `CANCELED` | 없음 (종착) |

**이 그림이 곧 코드입니다.** 새 상태를 추가할 때 고칠 곳은 `OrderStatus` 한 파일뿐입니다.
실제로 가상계좌를 나중에 추가했을 때, 이 설계 덕분에 서비스 코드는 거의 안 건드렸습니다.

---

## 16. ⚡ 기술 하이라이트 ③ — 결제가 실패하면 재고는?

**문제:** 주문 생성 시점에 재고를 이미 깎았는데, 결제가 실패하면?

```
주문 생성 → 재고 -1 → 결제 시도 → ❌ 실패
                                    ↑
                        여기서 아무것도 안 하면
                        재고는 영원히 잠긴 채 아무도 못 삼
```

**해결: 실패하면 주문을 CANCELED로 돌리고 재고를 되돌린다.**

```java
order.changeStatus(OrderStatus.CANCELED);
restoreStock(order);

private void restoreStock(Order order) {
  order.getOrderItems()
       .forEach(item -> item.getProduct().increaseStock(item.getQuantity()));
}
```

---

## 17. ⚡ 해결 ③ — 상태 머신이 여기서 한 번 더 일합니다

**한 걸음 더 나간 지점:** 사용자가 실패한 결제를 **새로고침해서 두 번 호출하면?**

재고가 **두 번** 복구되어 없던 재고가 생겨납니다. (더 위험한 종류의 버그입니다)

```java
// ORDERED → CANCELED 전이만 허용되므로,
// 이미 CANCELED인 주문에 pay()를 다시 호출하면 changeStatus에서 막힌다.
order.changeStatus(OrderStatus.CANCELED);   // ← 두 번째 호출은 여기서 예외
restoreStock(order);                        // ← 그래서 여기까지 오지 못한다
```

**상태 머신이 이중 복구를 구조적으로 차단합니다.**
별도의 "이미 취소했나?" 플래그가 필요 없습니다 — 상태 자체가 그 정보를 갖고 있으니까요.

> 💡 세 하이라이트가 여기서 만납니다: **상태 머신 + 트랜잭션 + 도메인 불변식**

---

## 18. 외부 연동 — 전략 패턴으로 감싼 이유

```java
public interface PaymentProcessor {
  ConfirmResult confirm(String paymentKey, String tossOrderId, int amount);
  ConfirmResult checkStatus(String paymentKey);   // 가상계좌 입금 확인(폴링)
}
```

| 구현체 | 쓰이는 곳 |
|---|---|
| `TossPaymentProcessor` | 실제 운영 — 토스페이먼츠 API 호출 |
| 테스트용 Mock | 단위 테스트 — 네트워크 없이 성공/실패 시나리오 재현 |

**왜 인터페이스로 뺐나:** 이게 없으면 `OrderService` 테스트가 **매번 실제 결제 API를 때립니다.**
느리고, 돈이 들고, 네트워크가 죽으면 테스트도 같이 죽습니다.

> 💡 "테스트하기 어렵다"는 신호는 대개 **설계가 덜 분리됐다는 신호**입니다.

---

## 19. 가상계좌 — 웹훅 대신 폴링을 고른 이유

가상계좌는 발급 후 **실제 입금까지 시간이 걸립니다.** 입금을 어떻게 아는가?

| | 웹훅 (실무 표준) | **폴링 (선택)** |
|---|---|---|
| 방식 | 토스가 내 서버로 알려줌 | 내가 토스에 주기적으로 물어봄 |
| 실시간성 | ✅ 즉시 | 조회 시점에만 |
| 로컬 개발 | ❌ 외부에서 내 PC로 들어와야 함 (ngrok 등 터널 필요) | ✅ 그냥 됨 |
| 재현성 | 팀원 PC마다 터널 설정 필요 | ✅ 클론하면 바로 동작 |

**판단:** 이 프로젝트의 원칙이 "클론 → `docker compose up` → 바로 동일하게 동작"이었습니다.
웹훅은 그 원칙을 깨뜨립니다. **실무 표준을 알면서도 제약에 맞춰 다른 선택을 했습니다.**

---

## 20. 인증과 인가 — 다른 문제입니다

```java
// URL 패턴 레벨 (SecurityConfig)
.requestMatchers("/api/admin/**").hasRole("ADMIN")

// 메서드 레벨 (AdminOrderController) — 이중 방어
@PreAuthorize("hasRole('ADMIN')")
@PatchMapping("/{orderId}/status")
public OrderResponse changeStatus(...) { ... }
```

**왜 두 번 막나:** URL 패턴은 나중에 경로를 리팩터링하다 **실수로 뚫릴 수 있습니다.**
메서드 레벨 검사는 경로가 바뀌어도 따라다닙니다.

**세션 vs JWT — 세션을 골랐습니다.**
단일 서버 + 같은 오리진 프록시 구조에서는 세션이 더 단순하고 안전합니다.
(토큰 탈취·만료·갱신 관리 문제를 아예 안 만듦.) 서버를 여러 대로 늘릴 때 JWT를 검토하면 됩니다.

---

## 21. 테스트 전략 — 3계층

| 계층 | 방식 | 검증하는 것 |
|---|---|---|
| `service/` | `@ExtendWith(MockitoExtension.class)` | **비즈니스 로직** — 의존성은 전부 Mock |
| `repository/` | `@DataJpaTest` (실제 H2) | **쿼리가 실제로 도는가** — JPQL 오타·연관관계 |
| `controller/` | `@WebMvcTest` + `@Import(SecurityConfig)` | **HTTP 계약 + 인가** — 상태코드·JSON·권한 |

**왜 나누나:** 전부 통합 테스트로 짜면 느리고, 깨졌을 때 **어디가 문제인지 모릅니다.**
서비스 테스트가 깨지면 로직 문제, 리포지토리 테스트가 깨지면 쿼리 문제 — 범위가 좁혀집니다.

---

## 22. 테스트 결과 (2026-08-08 실측)

| 구분 | 개수 | 상태 |
|---|---:|---|
| 백엔드 (JUnit 5 + Mockito) | **279** | ✅ 전부 통과 |
| 프론트엔드 (Vitest + Testing Library) | **114** | ✅ 전부 통과 |
| E2E 스모크 (Playwright) | **9** | ✅ 전부 통과 |
| **합계** | **402** | |

**JaCoCo 커버리지** — 제외(exclude) 규칙 없이 **전체 클래스 대상**

| 지표 | 수치 |
|---|---|
| 라인 | **98.8%** (1,059줄 중 13줄 미커버) |
| 브랜치 | **87.9%** (165개 중 20개 미커버) |
| CI 하드 게이트 | 60% 미만이면 **빌드 실패** |

---

## 23. E2E가 필요했던 이유

단위 테스트 393개가 전부 통과해도 **못 잡는 것**이 있습니다:

- 프론트가 부르는 URL과 백엔드가 여는 URL이 **서로 다를 때**
- 로그인 세션 쿠키가 **프록시를 못 넘어갈 때**
- 관리자 화면이 일반 회원에게 **그냥 보일 때** (백엔드는 막았는데 화면이 안 막음)

각 계층은 완벽한데 **이어붙인 지점**이 깨지는 경우입니다.

**핵심 사용자 경로 5개**를 브라우저로 실제 클릭합니다 (`frontend/e2e/`):
홈 · 목록→상세 · 회원가입 · 장바구니 · 관리자

---

## 24. CI 파이프라인 — 배포를 막는 게 목적

```
git push
   │
   ├──▶ [backend]   ./gradlew check + 커버리지 게이트(60%)
   ├──▶ [frontend]  포맷 검사 + 단위 테스트 + 빌드
   └──▶ [e2e]       DB+백엔드+프론트를 러너에 전부 띄우고 브라우저 검증
                              │
                    셋 다 통과해야만 ↓
                          [deploy]
                    Azure Container Apps
```

**`needs: [backend, frontend, e2e]`** — 하나라도 실패하면 **배포 잡이 아예 실행되지 않습니다.**

> 💡 목표는 "테스트를 돌리는 것"이 아니라
> **"배포 후에 발견하던 결함을 배포 전에 차단하는 것"**입니다.

---

## 25. 배포 — "git에 없으면 없는 것"

**Azure 인프라 전체가 `azure/setup.sh` 한 파일에 스크립트로 남아 있습니다.**

```bash
az containerapp env create ...          # 실행 환경
az postgres flexible-server create ...  # DB
az containerapp create ...              # 백엔드 / 프론트
az postgres ... firewall-rule create    # DB는 백엔드에서만 접근
```

**왜 이렇게 했나:** 포털에서 클릭으로 만들면 **git에 아무것도 안 남습니다.**
"어떻게 만들었더라?"를 6개월 뒤의 내가 알 수 없고, 다시 만들 수도 없습니다.

같은 원칙을 데이터에도 적용했습니다 — 상품·데모 계정은 `DevDataInitializer`가 **멱등하게** 시드합니다.
(`count() > 0` 대신 **항목별 `existsBy`** — 그래야 나중에 상품을 추가해도 들어갑니다.)

---

## 26. 실패에서 배운 것 ① — 커버리지 98%가 못 잡은 버그

**토스페이먼츠 가상계좌 입금기한(`dueDate`) 파싱 실패**

```
토스 응답:  "2026-07-29T23:59:59+09:00"
                                 ^^^^^^ 시간대 오프셋
내 코드:    LocalDateTime.parse(...)   → ❌ 예외
올바른 것:  OffsetDateTime.parse(...)  → ✅
```

**왜 테스트가 못 잡았나:** 테스트는 `MockRestServiceServer`로 **내가 만든 가짜 응답**을 씁니다.
가짜 응답에 오프셋을 안 넣었으니, 테스트는 영원히 통과합니다.

> 💡 **커버리지는 "내가 쓴 코드를 얼마나 실행했나"이지, "내 가정이 맞나"가 아닙니다.**
> 외부 API의 실제 응답 계약은 모킹으로 검증할 수 없습니다 — 한 번은 진짜로 붙여봐야 합니다.

---

## 27. 실패에서 배운 것 ② — 스키마 드리프트

**`OrderStatus`에 `WAITING_FOR_DEPOSIT`을 추가했더니, 테스트는 전부 통과하는데 브라우저로 실제 주문을 넣자 INSERT가 실패했습니다.**
(커밋 `1488c9e` — 가상계좌 기능을 브라우저로 검증하다 발견한 3개 버그 중 하나)

원인: `spring.jpa.hibernate.ddl-auto=update`는 **컬럼은 추가해도 기존 CHECK 제약조건은 갱신하지 않습니다.**

```sql
-- DB에 남아 있던 옛날 제약조건
CHECK (status IN ('ORDERED','PAID','SHIPPING','DELIVERED','CANCELED'))
--                 새로 추가한 WAITING_FOR_DEPOSIT이 없음 → INSERT 거부
```

**테스트는 매번 새 H2로 시작하니 구조적으로 못 잡습니다.**
(새 DB에는 항상 최신 제약조건이 생기니까요.)

> 💡 `ddl-auto=update`는 개발 편의 기능이지 **마이그레이션 도구가 아닙니다.**
> Flyway가 왜 필요한지를 문서가 아니라 **장애로 배웠습니다.**

---

## 28. 이 프로젝트에서 남은 것

기능 목록보다 오래 남을 것들:

1. **"왜 이걸 골랐나"를 말할 수 있게 됐다**
   낙관적 락 vs 비관적 락, 세션 vs JWT, 웹훅 vs 폴링 —
   전부 **정답이 아니라 트레이드오프**였고, 제약을 근거로 골랐습니다.

2. **테스트는 통과 도장이 아니라 설계 피드백이다**
   테스트하기 어려운 코드는 대개 분리가 덜 된 코드였습니다. (→ `PaymentProcessor`)

3. **재현되지 않으면 존재하지 않는다**
   인프라도, 데이터도, 배포도 전부 코드로 남겨야 6개월 뒤의 내가 쓸 수 있습니다.

---

## 29. 감사합니다

# MINS Farmers Market

🔗 **https://www.minsdev.works**
💻 **github.com/johnjoseph1990/KDT-ShoppingMallProject**

**질문 받겠습니다.**

---

## 부록 A. 예상 질문 대비

**Q. 왜 JWT가 아니라 세션인가요?**
단일 서버 + 같은 오리진 프록시 구조라 세션이 더 단순하고 안전합니다. 토큰 탈취·만료·갱신 관리 문제를 아예 만들지 않습니다. 서버를 수평 확장하게 되면 그때 JWT(또는 Redis 세션 저장소)를 검토하는 게 맞다고 봅니다.

**Q. 커버리지 98%인데 그럼 버그가 없나요?**
아니요. 실제로 커버리지가 못 잡은 버그를 두 개 겪었습니다(슬라이드 26·27). 커버리지는 "코드를 실행했나"를 재는 지표지 "내 가정이 맞나"를 재는 지표가 아닙니다.

**Q. 리뷰 키워드 추출은 어떻게 하나요?**
`KeywordExtractor`가 사전 기반 매칭으로 합니다. 동의어·오탈자에 약한 한계가 있고 주석으로 자인해뒀습니다. 개선하려면 형태소 분석기(Komoran, Okt 등) 도입이 다음 단계입니다.

**Q. 재시도 3번이 다 실패하면요?**
마지막 예외가 그대로 전파되어 `GlobalExceptionHandler`가 409 Conflict로 응답합니다. 사용자에게는 "잠시 후 다시 시도해 주세요"로 안내됩니다.

**Q. 동시성 테스트는 어떻게 했나요?**
`OrderConcurrencyTest`에서 스레드 풀로 동시에 주문을 밀어넣고, 최종 재고와 성공 주문 수가 일치하는지 검증합니다.

**Q. 더 개선한다면 무엇을 하시겠어요?**
세 가지가 남아 있습니다. ① **Flyway 도입** — 슬라이드 27의 스키마 드리프트로 필요성은 확인했으나 아직 적용 전입니다. ② **성능 최적화** — N+1 쿼리·인덱스 점검이 남아 있습니다(베스트 상품 쿼리는 이미 한 번 해결했습니다). ③ **프론트/백 테스트 균형** — 백엔드 279개에 비해 프론트 114개로 상대적으로 얇습니다.

---

## 부록 B. 기술 스택

| 구분 | 사용 기술 |
|---|---|
| Backend | Java 17, Spring Boot 3, Spring Data JPA, Spring Security |
| Frontend | React 18, Vite, React Router, Axios, 자체 CSS(사용자 화면) + Vapor(goorm DS, 관리자 화면) |
| Database | PostgreSQL (테스트는 H2) |
| 결제 | 토스페이먼츠 (카드 · 가상계좌) |
| Infra | Azure Container Apps, Container Registry, PostgreSQL Flexible Server, Blob Storage |
| CI/CD | GitHub Actions (4 job: backend / frontend / e2e / deploy) |
| 테스트 | JUnit 5, Mockito, Vitest, Testing Library, Playwright |
| 품질 | Spotless(Google Java Format), Prettier, JaCoCo |
