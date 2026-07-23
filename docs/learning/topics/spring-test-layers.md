# Spring 테스트 계층 구조

> 작성 배경: MemberService / MemberController 테스트 케이스 추가 작업 (2026-07-22)

---

## 전체 흐름도

```
실제 요청 흐름 (런타임)
───────────────────────────────
HTTP 요청
   ↓
Controller   (입력 검증, 인증 확인)
   ↓
Service      (비즈니스 로직)
   ↓
Repository   (DB 접근)
   ↓
DB

테스트 흐름 (각 계층을 독립적으로 검증)
───────────────────────────────
@WebMvcTest          → Controller만 테스트 (Service는 Mock)
@ExtendWith(Mockito) → Service만 테스트 (Repository는 Mock)
@DataJpaTest         → Repository만 테스트 (실제 H2 DB)
```

---

## 핵심 개념

### 1. `@WebMvcTest` — 컨트롤러 슬라이스 테스트

**초보자 설명**
"Spring 전체를 띄우지 않고, 컨트롤러 레이어만 잘라서(slice) 테스트하는 방법"
전체 서버를 켜면 느리니까, 딱 HTTP 요청/응답 부분만 테스트한다.

**어느 부분**
Spring MVC 레이어. `@Controller`, `@RestController`를 테스트 대상으로 한정.
Service, Repository는 `@MockitoBean`으로 가짜 객체로 교체.

**실무 활용**
- API 계약(상태코드, JSON 구조) 검증
- 인증/인가 규칙 테스트 (미로그인 시 401인지 등)
- 입력값 유효성 검사(@Valid) 결과 확인

```java
@WebMvcTest(MemberController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;           // 가짜 HTTP 요청을 보내는 도구

    @MockitoBean
    private MemberService memberService;  // Service는 가짜로 교체
}
```

---

### 2. `@ExtendWith(MockitoExtension.class)` — 서비스 단위 테스트

**초보자 설명**
"JUnit5에게 Mockito(가짜 객체 라이브러리)를 사용하겠다고 알려주는 설정"
이것 하나면 `@Mock`, `@InjectMocks` 어노테이션이 자동으로 동작한다.

**어느 부분**
JUnit5 확장 포인트 + Mockito 연동. 서비스 레이어 단위 테스트.

**실무 활용**
DB 없이 서비스 로직만 빠르게 검증. CI 파이프라인에서 수백 개 테스트를 수초에 통과시키는 핵심.

```java
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;  // 가짜 Repository

    @Mock
    private PasswordEncoder passwordEncoder;     // 가짜 암호화 도구

    @InjectMocks
    private MemberService memberService;         // 위 가짜들이 주입된 진짜 Service
}
```

---

### 3. `given(...).willReturn(...)` — BDD 스타일 Mock 설정

**초보자 설명**
"이 메서드가 호출되면, 이 값을 돌려줘"라고 약속시키는 것.
실제 DB 조회 없이 원하는 결과를 만들어낸다.

**어느 부분**
Mockito의 BDD(Behavior Driven Development) API.
`when(...).thenReturn(...)` 과 동일하지만 더 읽기 쉬운 형태.

**실무 활용**
```java
// "findById(1L)을 호출하면 member 객체를 돌려줘"
given(memberRepository.findById(1L)).willReturn(Optional.of(member));

// "matches("wrongPass", ...)를 호출하면 false를 돌려줘"
given(passwordEncoder.matches("wrongPass", "encoded")).willReturn(false);
```

---

### 4. `assertThatThrownBy` — 예외 검증

**초보자 설명**
"이 코드를 실행했을 때 예외가 발생하는지, 어떤 예외인지 확인하는 방법"

**어느 부분**
AssertJ 라이브러리의 예외 검증 API. JUnit의 `assertThrows`보다 읽기 쉽다.

**실무 활용**
방어 로직(잘못된 비밀번호, 없는 회원 등)이 올바르게 동작하는지 보장.

```java
assertThatThrownBy(() -> memberService.update(99L, request))
    .isInstanceOf(ResourceNotFoundException.class)
    .hasMessageContaining("존재하지 않는");
```

---

### 5. `willThrow(...)` — 예외 발생 Mock 설정

**초보자 설명**
"이 메서드가 호출되면 예외를 던져라"라고 지시하는 것.
컨트롤러가 Service 예외를 받아 올바른 HTTP 상태코드로 변환하는지 테스트할 때 사용.

**어느 부분**
Mockito stubbing의 예외 버전. `GlobalExceptionHandler`와 함께 테스트.

**실무 활용**
```java
// Service가 예외를 던졌을 때 Controller가 404로 응답하는지 확인
willThrow(new ResourceNotFoundException("존재하지 않는 회원"))
    .given(memberService).update(eq(1L), any());

mockMvc.perform(put("/api/members/me")...)
    .andExpect(status().isNotFound());  // 404 확인
```

---

## 이번 작업에서 추가된 테스트 케이스

### MemberServiceTest — 추가 케이스

| 테스트 이름 | 검증 내용 | 왜 필요한가 |
|------------|---------|-----------|
| `update_이름과_비밀번호_동시변경_성공` | 두 필드를 한 번에 바꿀 수 있는지 | 동시 변경 시 로직 충돌 가능성 |
| `update_존재하지않는_회원_예외발생` | 없는 ID → ResourceNotFoundException | 방어 로직 존재 여부 |
| `update_currentPassword_null인데_newPassword_요청시_예외발생` | 현재 비번 없이 새 비번만 보내면 거부 | 보안: 비밀번호 우회 방지 |
| `delete_존재하지않는_회원_예외발생` | 없는 ID 탈퇴 시 예외 발생, delete 미호출 | 잘못된 삭제 방지 |

### MemberControllerTest — 추가 케이스

| 테스트 이름 | 검증 내용 | 왜 필요한가 |
|------------|---------|-----------|
| `미로그인_탈퇴요청_401` | 비인증 DELETE → 401 | 인증 없이 탈퇴 불가 확인 |
| `존재하지않는_회원_수정요청_404` | Service 예외 → HTTP 404 변환 | GlobalExceptionHandler 동작 확인 |

---

## 자주 하는 실수

1. **`@MockitoBean` vs `@Mock` 혼용**: `@WebMvcTest`에서는 `@MockitoBean`, `@ExtendWith(Mockito...)`에서는 `@Mock` 사용. 섞으면 빈 주입 오류 발생.
2. **테스트가 UP-TO-DATE로 건너뜀**: Gradle이 이전 결과를 캐시함. 강제 재실행은 `./gradlew cleanTest test`.
3. **`given` 없이 Mock 호출**: stubbing 안 된 메서드는 기본값(null, 0, false) 반환. 의도치 않은 테스트 통과 원인.

---

## 셀프 체크 (답을 보지 말고 먼저 떠올려 볼 것)

> 이 문서를 다시 열 때마다, 본문을 읽기 전에 아래 질문에 먼저 답해보세요.

1. `@WebMvcTest` / `@ExtendWith(MockitoExtension.class)` / `@DataJpaTest` — 각각 어느 계층을 테스트하고, 나머지 계층을 어떻게 처리하는가?
2. `@MockitoBean`과 `@Mock`의 차이는? 어느 어노테이션을 어느 테스트에서 쓰는가?
3. `given(repo.findById(1L)).willReturn(Optional.of(member))`의 역할을 한 문장으로 설명하라.
4. `assertThatThrownBy`로 예외를 검증할 때 체크해야 하는 두 가지는?
5. `willThrow(new SomeException()).given(service).method(any())`를 왜 쓰는가? (어떤 시나리오를 테스트하는 목적인가)
