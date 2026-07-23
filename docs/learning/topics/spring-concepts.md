# 이 프로젝트에서 배우는 Spring 개념 정리

이 문서는 실제로 이 프로젝트(`backend/`)에 쓰이고 있는 Spring/JPA 개념만 정리한 학습 자료다. 코드에 없는 개념(예: `@Component`, `@ManyToMany`, `@MockBean`)은 다루지 않는다 — 모르는 개념이 나오면 실제로 쓰일 때 그 파일 주석에서 설명하고 이 문서에 추가한다.

## 1. 의존성 주입 (DI) — 생성자 주입만 사용

이 프로젝트는 `@Autowired` 필드 주입을 **한 곳도 쓰지 않는다.** 전부 생성자 주입이다.

```java
// OrderService.java (2026-07-23 리팩토링 후 — PaymentProcessor 분리)
public OrderService(
    OrderRepository orderRepository,
    CartItemRepository cartItemRepository,
    MemberRepository memberRepository,
    PaymentRepository paymentRepository,
    PaymentProcessor paymentProcessor) {  // 결제 결정 로직을 인터페이스로 분리
  this.orderRepository = orderRepository;
  ...
  this.paymentProcessor = paymentProcessor;
}
```

**왜 생성자 주입인가**: 필드가 `final`로 선언되어 객체 생성 이후 절대 바뀌지 않는다는 걸 컴파일러가 보장해준다. 또 테스트에서 `new OrderService(mock1, mock2, ...)`로 직접 만들 수 있어 Mockito 테스트 작성이 쉬워진다(아래 8번 참고).

**실무 팁**: 생성자 파라미터가 4개를 넘으면 "이 서비스가 너무 많은 책임을 지고 있는 게 아닌가"를 의심하는 신호다. `OrderService`는 이제 5개인데, `PaymentProcessor`를 인터페이스로 분리한 덕분에 오히려 책임이 명확해진 사례다 — "결제 성공 여부를 어떻게 결정할지"는 이제 `OrderService`가 알 필요가 없다. 아키텍처 개선 맥락은 `docs/learning/notes/architecture-review-2026-07-14.md` 참고.

**전략 패턴 미리보기**: `PaymentProcessor`가 인터페이스이기 때문에, 운영에서는 `MockPaymentProcessor`(90% 확률), 테스트에서는 Mockito Mock(`willReturn(true/false)`으로 고정)을 주입할 수 있다. 이처럼 "행동을 인터페이스로 분리해서 교체 가능하게 만드는 패턴"을 전략 패턴이라고 한다.

## 2. 계층 어노테이션

| 어노테이션 | 이 프로젝트에서의 역할 |
|---|---|
| `@RestController` | 컨트롤러 6개 전부 (`@Controller`+`@ResponseBody`를 합친 것 — 메서드 반환값이 뷰 이름이 아니라 그대로 JSON 응답 본문이 됨) |
| `@Service` | 서비스 6개 전부. "이 클래스는 비즈니스 로직을 담당한다"는 표시이자, 스프링이 빈으로 등록해서 컨트롤러에 주입 가능하게 해줌 |
| `@Configuration` | `SecurityConfig`, `DevDataInitializer` — `@Bean` 메서드를 담는 설정 클래스 |

**`@Repository`가 안 보이는 이유**: `repository/*.java`는 전부 `JpaRepository<Entity, ID>`를 상속하는 **인터페이스**다. Spring Data JPA가 앱 시작 시 이 인터페이스들을 스캔해서 구현체를 자동 생성 + 빈 등록까지 해주기 때문에, `@Repository`를 직접 붙일 필요가 없다 (붙여도 무방하지만 이 프로젝트는 생략하는 스타일을 택함).

## 3. 엔티티 매핑 (JPA)

### 기본 키
모든 엔티티가 `@Id` + `@GeneratedValue(strategy = GenerationType.IDENTITY)`를 쓴다. DB가 auto-increment로 PK를 채번한다는 뜻.

### 연관관계
```java
// Order.java
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
private List<OrderItem> orderItems = new ArrayList<>();
```
- `mappedBy`: "이 연관관계의 주인은 내가 아니라 상대방(`OrderItem.order` 필드)"이라는 표시
- `cascade = CascadeType.ALL`: `Order`를 저장/삭제하면 그 안의 `OrderItem`들도 같이 저장/삭제됨
- `orphanRemoval = true`: `orderItems` 리스트에서 항목을 빼면 DB에서도 그 `OrderItem`이 삭제됨

`@ManyToOne`(예: `OrderItem.product`)은 기본 fetch 전략이 **EAGER**(즉시 로딩)이고, `@OneToMany`(예: `Order.orderItems`)는 기본이 **LAZY**(지연 로딩)다. 이 프로젝트는 `fetch = ...`를 명시한 곳이 한 군데도 없어서 전부 기본값을 그대로 쓰고 있다.

**실무에서 중요한 이유 (N+1 문제)**: `OrderService.getOrders()`처럼 `Order` 목록을 조회한 뒤 각 `Order`의 `orderItems`(LAZY)를 순회하면, Order가 10개면 추가 쿼리가 10번 더 나갈 수 있다 — 이게 "N+1 문제"다. 지금 이 프로젝트에는 `@EntityGraph`나 fetch join 같은 해결책이 아직 적용되어 있지 않다. 트래픽이 적은 학습 프로젝트에서는 체감되지 않지만, 실무에서는 성능 이슈의 단골 원인이라 알아두면 좋다.

### 낙관적 락
```java
// Product.java
@Version
private Long version;
```
두 사람이 동시에 같은 상품 재고를 차감하려 할 때, 버전이 안 맞으면 `OptimisticLockingFailureException`이 터진다. `GlobalExceptionHandler`가 이걸 잡아서 "다른 주문과 재고 처리가 충돌했습니다. 다시 시도해주세요."로 응답한다.

## 4. DTO — record + Bean Validation

```java
// SignupRequest.java
public record SignupRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.") String password,
    @NotBlank String name) {}
```
- **`record`**: 필드, 생성자, `getter`(`email()`처럼 필드명과 같은 메서드), `equals/hashCode/toString`을 자동 생성해주는 불변 데이터 클래스. 이 프로젝트의 DTO 15개가 전부 `record`이고, 일반 `class` DTO는 없다.
- **Bean Validation** (`@NotBlank`, `@Email`, `@Size`, `@Min`, `@Max`, `@NotNull`): 컨트롤러 파라미터에 `@Valid`를 붙이면(예: `ProductController`) 이 조건들을 스프링이 자동 검사하고, 실패하면 `MethodArgumentNotValidException`을 던진다 → `GlobalExceptionHandler`가 받아서 400 응답으로 변환.

응답 DTO는 `from(entity)` 정적 팩토리 메서드로 엔티티를 변환한다 (예: `OrderResponse.from(order)`). 이렇게 하면 엔티티가 API 응답으로 직접 나가는 걸 막을 수 있다 (비밀번호 같은 민감 필드 누출 방지).

## 5. 예외 처리 — `@RestControllerAdvice`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
  }
  ...
}
```
`@RestControllerAdvice`가 붙은 클래스는 앱 전역에서 발생하는 특정 예외를 가로채 공통된 형식으로 응답을 만들어준다. 각 컨트롤러/서비스마다 `try-catch`를 반복하지 않아도 되는 이유다. 이 프로젝트는 9개의 예외 타입을 각각 다른 HTTP 상태 코드(400/403/404/409)로 매핑한다.

## 6. 트랜잭션 — `@Transactional`

```java
@Service
@Transactional(readOnly = true)
public class OrderService {
  @Transactional
  public OrderResponse createOrder(...) { ... }
```
클래스 전체에는 `readOnly = true`(조회 전용, 변경 감지 오버헤드를 줄여 약간 더 빠름)를 기본값으로 깔고, 실제로 DB를 바꾸는 메서드(`createOrder`, `pay` 등)에만 개별 `@Transactional`을 덮어써서 "쓰기 가능" 모드로 전환한다. 6개 서비스 전부 이 패턴을 동일하게 따른다.

**실무 팁**: `readOnly = true`인 메서드 안에서 실수로 엔티티 값을 바꾸면(예: `product.decreaseStock(...)`) 그 변경이 DB에 반영되지 않아 조용히 무시된다 — 흔한 실수 유형이니 "이 메서드가 데이터를 바꾸는가?"를 먼저 판단하고 어노테이션을 다는 습관이 중요하다.

## 7. Lombok — 최소한만 사용

모든 엔티티가 이 조합만 쓴다:
```java
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product { ... }
```
- `@Getter`: 필드마다 손으로 getter를 안 써도 되게 자동 생성
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)`: JPA는 프록시 생성을 위해 기본 생성자가 필요한데, `public`으로 열어두면 아무 곳에서나 빈 엔티티를 만들 수 있어 위험하므로 `protected`로 제한

`@Setter`, `@Builder`, `@Data`는 의도적으로 안 쓴다 — setter가 없으면 `product.decreaseStock(quantity)`처럼 의미 있는 메서드로만 상태를 바꾸게 강제되어, "누가 언제 왜 값을 바꿨는지" 추적하기 쉬워진다.

## 8. 테스트 — 계층별로 다른 어노테이션

| 계층 | 어노테이션 | 이 프로젝트 예시 |
|---|---|---|
| Service | `@ExtendWith(MockitoExtension.class)` + `@Mock` + `@InjectMocks` | `OrderServiceTest.java` — 진짜 DB 없이 레포지토리를 가짜(Mock)로 대체 |
| Repository | `@DataJpaTest` | `MemberRepositoryTest.java` — 실제 H2 DB에 저장/조회까지 검증 |
| Controller | `@WebMvcTest` + `@Import({SecurityConfig.class, GlobalExceptionHandler.class})` | `ProductControllerTest.java` — MockMvc로 HTTP 요청/응답만 검증, 서비스는 껍데기만 로드 |

**`@MockBean`을 안 쓰는 이유**: `@MockBean`은 스프링 컨텍스트 전체를 띄우면서 그 안의 특정 빈만 가짜로 바꾸는 방식이라 무겁다. 이 프로젝트는 Service 테스트에서 스프링 컨텍스트를 아예 띄우지 않고 순수 자바 객체(`new OrderService(mock, mock, ...)`)로 테스트하기 때문에 `@MockBean`이 필요 없다 — 1번(생성자 주입)이 있어서 가능한 방식이다.

## 참고
- 이 문서는 2026-07-14 기준 코드 조사 결과다. 이후 코드가 바뀌면 이 문서도 같이 갱신할 것.
- 아키텍처 관점의 개선 우선순위는 `docs/learning/notes/architecture-review-2026-07-14.md` 참고.

---

## 셀프 체크 (답을 보지 말고 먼저 떠올려 볼 것)

> 이 문서를 다시 열 때마다, 본문을 읽기 전에 아래 질문에 먼저 답해보세요.

1. 이 프로젝트가 `@Autowired` 필드 주입 대신 생성자 주입만 쓰는 이유 두 가지는?
2. `JpaRepository`를 상속한 인터페이스에 `@Repository`를 붙이지 않아도 되는 이유는?
3. `@OneToMany`의 `mappedBy`, `cascade`, `orphanRemoval` 각각이 의미하는 바를 한 문장씩 설명하라.
4. N+1 문제란 무엇인가? 이 프로젝트의 어떤 코드에서 발생할 가능성이 있는가?
5. 서비스 클래스에 `@Transactional(readOnly = true)`를 기본으로 깔고, 쓰기 메서드에만 `@Transactional`을 덮어쓰는 이유는?
6. `@RestControllerAdvice`가 없다면 예외 처리 코드를 어디에 어떻게 써야 하는가? 왜 불편한가?
