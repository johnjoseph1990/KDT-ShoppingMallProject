# 관리자 기능 구현

> 작성 배경: SPRINT_PLAN.md 요구사항 "관리자 관점에서 상품 및 주문 내역을 관리할 수 있는
> 기능을 구현합니다. 주문 상태 변경, 재고 확인 등 운영 기능을 구성합니다" 완료 후 문서화 (2026-07-23)

---

## 전체 구조

```
HTTP 요청                  Spring Security 필터
GET /api/admin/orders  →  /api/admin/** hasRole("ADMIN") 검사
                              ↓ 통과
                          AdminOrderController
                              ↓
                          OrderService.getAllOrders(status, pageable)
                              ↓
                          OrderRepository.findAll(pageable)
                           or findByStatus(status, pageable)

프론트엔드
/admin              → PrivateRoute(adminOnly)
                          ↓ user.role === 'ADMIN'이면
                      AdminPage
                        ├─ ProductManager (상품 목록/등록/수정/삭제)
                        └─ OrderManager  (상태 필터 + 행별 상태 변경)
```

관련 파일 전체:

```
backend/src/main/java/
├── config/SecurityConfig.java               ← URL 패턴별 인가 규칙
├── controller/AdminOrderController.java      ← /api/admin/orders 진입점
└── service/OrderService.java                ← getAllOrders, changeOrderStatus

backend/src/test/java/
└── controller/AdminOrderControllerTest.java  ← 8개 시나리오 커버

frontend/src/
├── components/PrivateRoute.jsx               ← adminOnly 라우트 가드
├── pages/AdminPage.jsx                       ← 상품/주문 탭 관리자 UI
└── api/admin.js                              ← getAdminOrders, updateOrderStatus
```

---

## 1. Spring Security URL 기반 인가

### SecurityConfig 규칙 읽는 법

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/signup", "/api/auth/login").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
    .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
    .requestMatchers(HttpMethod.PUT,  "/api/products/**").hasRole("ADMIN")
    .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
    .requestMatchers("/api/admin/**").hasRole("ADMIN")   // 관리자 전용 API 전체
    .anyRequest().authenticated()
)
```

**순서가 중요하다**: 스프링 시큐리티는 위에서부터 매칭되는 첫 번째 규칙만 적용한다.
`GET /api/products/1`은 세 번째 줄(`GET /api/products/**`)에서 `permitAll()`로 허용되어
아래 `anyRequest().authenticated()`까지 내려가지 않는다.

### 이 프로젝트의 URL 규칙 요약

| URL 패턴 | HTTP 메서드 | 필요 조건 |
|---|---|---|
| `/api/auth/signup`, `/api/auth/login` | 모두 | 없음 (로그인 전 호출이라 필수) |
| `/api/products/**` | GET | 없음 (미로그인 사용자도 상품 조회 가능) |
| `/api/products/**/reviews` | POST | 로그인만 |
| `/api/products/**/reviews/**` | DELETE | 로그인만 |
| `/api/products/**` | POST, PUT, DELETE | ADMIN 권한 |
| `/api/admin/**` | 모두 | ADMIN 권한 |
| 나머지 모두 | 모두 | 로그인만 |

### `hasRole("ADMIN")`이 확인하는 것

스프링 시큐리티는 내부적으로 `ROLE_` 접두사를 붙여 비교한다.
`hasRole("ADMIN")` → 실제 비교 대상: `"ROLE_ADMIN"`.
DB에서 회원의 `role` 컬럼 값이 `ADMIN`이면, `MemberUserDetailsService`가
`new SimpleGrantedAuthority("ROLE_ADMIN")`으로 변환해 `SecurityContext`에 등록한다.

---

## 2. 401 vs 403 — 헷갈리는 HTTP 상태코드 구분

이 두 코드는 모두 "접근 실패"지만 **원인**이 다르다.

| 코드 | 의미 | 언제 발생하는가 | 이 프로젝트 테스트 |
|---|---|---|---|
| 401 Unauthorized | 신원 불명 — 로그인 안 함 | 인증 정보(세션/토큰)가 없거나 만료 | `@WithMockUser` 없이 요청 |
| 403 Forbidden | 신원 확인됨 — 하지만 권한 없음 | 로그인은 했지만 역할이 부족 | `@WithMockUser(roles = "USER")`로 요청 |

**기본값 문제와 수동 수정**: 스프링 시큐리티 기본 설정은 인증 실패(로그인 안 함)에도
403을 반환한다. HTTP 표준에 맞게 고치려면 `authenticationEntryPoint`를 명시해야 한다.

```java
// SecurityConfig.java
.exceptionHandling(exception ->
    exception.authenticationEntryPoint(
        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
// 이 설정이 없으면 "미인증_401" 테스트가 실제로는 403을 받아 실패한다
```

---

## 3. 컨트롤러 — 선택적 파라미터와 페이지네이션

```java
// AdminOrderController.java
@GetMapping
public Page<OrderResponse> getAllOrders(
    @RequestParam(required = false) OrderStatus status,
    // @PageableDefault: 요청에 page/size/sort가 없을 때 사용할 기본값 지정
    @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable) {
  return orderService.getAllOrders(status, pageable);
}
```

**`@RequestParam(required = false)`**: 쿼리스트링에 파라미터가 없으면 `null`로 넘겨준다.
기본값 `required = true` 상태에서 파라미터를 생략하면 400 오류가 난다 — 선택적 필터는
반드시 `required = false`를 명시해야 한다.

**`@PageableDefault`**: `?page=0&size=5&sort=createdAt,desc` 형태의 쿼리스트링이 없을 때
적용되는 기본값이다. 클라이언트가 페이지 파라미터를 생략해도 일관된 동작이 보장된다.

**서비스에서 null 조건 분기**:
```java
// status가 null이면 전체 조회, 있으면 상태별 필터링
public Page<OrderResponse> getAllOrders(OrderStatus status, Pageable pageable) {
    Page<Order> orders =
        status == null
            ? orderRepository.findAll(pageable)
            : orderRepository.findByStatus(status, pageable);
    return orders.map(OrderResponse::from);
}
```

---

## 4. 주문 상태 변경 — 도메인 내부의 상태 전이 유효성

```java
// AdminOrderController.java
@PatchMapping("/{orderId}/status")
public OrderResponse changeOrderStatus(
    @PathVariable Long orderId, @Valid @RequestBody OrderStatusUpdateRequest request) {
  return orderService.changeOrderStatus(orderId, request.status());
}

// OrderService.java
@Transactional
public OrderResponse changeOrderStatus(Long orderId, OrderStatus status) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new ResourceNotFoundException("주문을 찾을 수 없습니다. id=" + orderId));
    order.changeStatus(status);  // 도메인 메서드 안에서 전이 유효성 검사
    if (status == OrderStatus.CANCELED) {
        restoreStock(order);     // 취소 시 재고 복구 (stock-sync.md 참고)
    }
    return OrderResponse.from(order);
}
```

**왜 전이 유효성을 엔티티 안에 두는가**: 서비스/컨트롤러에서 직접 `if (status == X && newStatus == Y)` 검증을 하면 로직이 여러 곳에 흩어져 나중에 하나를 빠뜨리는 실수가 생긴다.
`Order.changeStatus()` 메서드 하나가 모든 변경 경로의 관문이 되어 규칙을 항상 강제한다.

잘못된 전이(예: ORDERED → DELIVERED 직행)는 `InvalidOrderStatusException`을 던지고
`GlobalExceptionHandler`가 이를 받아 400으로 응답한다.

---

## 5. 프론트엔드 — 관리자 라우트 가드

### `PrivateRoute.jsx`

```jsx
export default function PrivateRoute({ children, adminOnly = false }) {
  const { user } = useAuth()
  const location = useLocation()

  if (!user) return <Navigate to="/login" replace state={{ from: location }} />
  if (adminOnly && user.role !== 'ADMIN') return <Navigate to="/" replace />

  return children
}
```

- `!user` → 미로그인 사용자는 로그인 페이지로 이동.
  `state={{ from: location }}`으로 원래 가려던 URL을 함께 전달해 로그인 후 돌아올 수 있게 한다.
- `adminOnly && user.role !== 'ADMIN'` → 로그인은 했지만 ADMIN이 아니면 홈으로 리다이렉트

**왜 프론트에도 권한 검사가 필요한가**: 백엔드 Spring Security가 API를 막지만,
브라우저 주소창에 `/admin`을 직접 입력하면 UI가 렌더링을 시도한다.
`PrivateRoute`가 없으면 API 호출이 403으로 실패하는 동안 빈 화면이나 오류 UI가 노출된다.
단, **보안의 진짜 경계는 항상 백엔드**다 — 프론트 가드는 UX 레벨의 보조 장치다.

### 조건부 API 파라미터 스프레드

```js
// admin.js
export const getAdminOrders = (page = 0, status = null) =>
  axiosInstance.get('/admin/orders', {
    params: { page, ...(status && { status }) }
  })
```

`...(status && { status })` — 단축 평가(Short-circuit evaluation):
- `status`가 `null`이면: `null && {...}` → `null`(falsy) → 스프레드 시 아무것도 추가 안 됨 → `?page=0`만 전송
- `status`가 `"PAID"`이면: `"PAID" && { status: "PAID" }` → `{ status: "PAID" }` → 스프레드 → `?page=0&status=PAID` 전송

---

## 6. 테스트 전략 — 8개 시나리오

관리자 컨트롤러 테스트의 핵심은 "누가 요청하는가"에 따른 인가 결과 검증이다.

| 테스트 이름 | `@WithMockUser` | 기대 코드 | 무엇을 검증하는가 |
|---|---|---|---|
| `전체주문조회_ADMIN_200` | `roles = "ADMIN"` | 200 | 정상 응답 + JSON 구조 |
| `전체주문조회_상태필터_200` | `roles = "ADMIN"` | 200 | `?status=PAID` → 필터 동작 |
| `전체주문조회_미인증_401` | 없음 | 401 | 로그인 안 하면 접근 불가 |
| `전체주문조회_USER권한_403` | `roles = "USER"` | 403 | ADMIN이 아니면 막힘 |
| `주문상태변경_ADMIN_200` | `roles = "ADMIN"` | 200 | 상태 변경 성공 |
| `주문상태변경_미인증_401` | 없음 | 401 | 로그인 안 하면 접근 불가 |
| `주문상태변경_USER권한_403` | `roles = "USER"` | 403 | ADMIN이 아니면 변경 불가 |
| `주문상태변경_잘못된전이_400` | `roles = "ADMIN"` | 400 | 허용되지 않는 상태 전이 |

**왜 401과 403 케이스를 둘 다 작성해야 하는가**:
- `미인증_401`만 있으면: "로그인만 해도 관리자 API에 접근되는 게 아닌가?"를 검증하지 못한다.
- `USER권한_403`만 있으면: "로그인 안 한 경우 어떤 응답이 오는가?"를 검증하지 못한다.
- 두 케이스가 함께 있어야 SecurityConfig의 `hasRole("ADMIN")` 규칙이 실제로 두 방향에서 모두 막고 있음을 확인할 수 있다.

**`@WithMockUser(roles = "ADMIN")`이 하는 일**: 실제 DB 조회나 비밀번호 검증 없이,
Spring Security의 `SecurityContext`에 "ROLE_ADMIN 권한을 가진 인증된 사용자"를 직접 주입한다.
`@WebMvcTest`는 Spring MVC + Security 레이어만 로드하므로, 실제 로그인 흐름 없이
인가 규칙만 빠르게 검증할 수 있다.

---

## 자주 하는 실수

1. **401과 403을 혼동**: 테스트 케이스를 "미인증→401, 권한부족→403"으로 분리하지 않으면
   SecurityConfig가 두 경우를 제대로 구분하고 있는지 알 수 없다.

2. **`authenticationEntryPoint` 미설정**: 스프링 시큐리티 기본값은 인증 실패(로그인 안 함)도
   403으로 응답한다. `HttpStatusEntryPoint(UNAUTHORIZED)`를 등록하지 않으면
   "미인증_401" 테스트가 403을 받아 실패한다.

3. **`PrivateRoute`에 `adminOnly` 누락**: 백엔드가 API를 막더라도 프론트 라우트에
   `adminOnly`가 없으면 USER가 관리자 페이지 UI를 렌더링한다 (API 호출은 실패하더라도).
   빈 화면이나 오류 노출을 막으려면 라우트 레벨에서도 걸러야 한다.

4. **`@RequestParam`에 `required = false` 누락**: 선택적 필터 파라미터를 생략 가능하게
   만들려면 반드시 명시해야 한다. 기본값 `required = true`인 채로 두면 파라미터 없는
   요청이 400으로 실패한다.

---

## 셀프 체크 (답을 보지 말고 먼저 떠올려 볼 것)

> 이 문서를 다시 열 때마다, 본문을 읽기 전에 아래 질문에 먼저 답해보세요.

1. Spring Security가 URL 기반 인가 규칙을 적용하는 순서는? 왜 순서가 중요한가?
2. 401과 403의 차이는? 이 프로젝트에서 401이 기본으로 나오지 않는 이유와 해결 방법은?
3. `@RequestParam(required = false)`를 쓰면 파라미터가 없을 때 메서드 파라미터에 무슨 값이 들어오는가?
4. `@WithMockUser(roles = "ADMIN")`은 실제로 어떤 일을 하는가? DB 조회나 비밀번호 검증이 일어나는가?
5. `...(status && { status })`에서 `status`가 `null`일 때와 `"PAID"`일 때 각각 어떻게 다른가?
6. 프론트엔드 `PrivateRoute`의 `adminOnly` 검사가 보안의 진짜 경계가 될 수 없는 이유는? 그렇다면 그 역할은 무엇인가?
