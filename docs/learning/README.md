# 학습 문서 인덱스

KDT 쇼핑몰 프로젝트의 **모든 학습 자료가 모이는 유일한 홈**입니다.
프로젝트 산출물(ERD, 기능 명세, 스프린트 일정)은 [`document/`](../../document)에 있습니다.

## 폴더 구조

```
docs/learning/
├── README.md          ← 이 파일 (유일한 인덱스)
├── CONCEPT_INDEX.md   ← 개념 → 강의 위치 역방향 사전
├── LEARNING_PLAN.md   ← 제출 후 16~20주 장기 학습 계획
├── notes/             ← 날짜별 학습노트 (그날 배운 것, 쌓기만 함)
├── topics/            ← 주제별 정리 (반복 등장한 개념을 추려서 승격)
└── slides/            ← 발표용 파생물 (원본은 항상 topics/의 md)
```

**운영 규칙**

1. 작업하며 배운 것은 `notes/YYYY-MM-DD_제목.md`로 기록
2. 같은 개념이 노트에 반복 등장하면 `topics/`의 주제 문서로 승격
3. 새 문서를 만들면 반드시 이 README 목록에 추가
4. **슬라이드는 md에서 생성한 파생물** — 내용 수정은 항상 md 원본에만 하고, 슬라이드는 필요할 때 재생성
5. 주 1회(금요일) 그 주의 `notes/`를 훑고 2번 승격 여부 판단

---

## 주제별 정리 (`topics/`)

### Git / 협업 도구

| 문서 | 핵심 키워드 | 최종 수정 |
|------|-----------|----------|
| [Git 기초](./topics/git-basics.md) | commit, push, pull, clone, conflict, staging | 2026-07-23 |

### Spring Boot 백엔드

| 문서 | 핵심 키워드 | 최종 수정 |
|------|-----------|----------|
| [Spring 핵심 개념](./topics/spring-concepts.md) | DI, 생성자 주입, 전략 패턴, @Transactional, @Version | 2026-07-23 |
| [테스트 계층 구조](./topics/spring-test-layers.md) | @WebMvcTest, @ExtendWith, Mockito, 결정론적 테스트, 전략 패턴 | 2026-07-23 |
| [재고 동기화 설계](./topics/stock-sync.md) | 낙관적 락, @Version, restoreStock, @DataJpaTest, 3계층 테스트 | 2026-07-23 |
| [배송지 관리 구현](./topics/address-management.md) | @ManyToOne, JPA 네이밍, @Transactional, OWASP A01 | 2026-07-22 |

### React 프론트엔드

| 문서 | 핵심 키워드 | 최종 수정 |
|------|-----------|----------|
| [마이페이지 리디자인](./topics/frontend-mypage-redesign.md) | 2컬럼 레이아웃, disclosure 패턴, Context API | 2026-07-22 |

---

## 날짜별 학습노트 (`notes/`)

| 날짜 | 문서 | 내용 |
|------|------|------|
| 2026-07-14 | [아키텍처 리뷰](./notes/architecture-review-2026-07-14.md) | OrderService 책임 과다 등 개선 우선순위 |
| 2026-07-23 | [인증/주문 요구사항 검증](./notes/2026-07-23_인증주문요구사항검증_학습노트.md) | 로그인 401→500 회귀, 관리자 취소 재고 미복구 버그 |
| 2026-07-23 | [PaymentProcessor 리팩토링](./notes/2026-07-23_PaymentProcessor_리팩토링_학습노트.md) | 전략 패턴, 결정론적 테스트, @InjectMocks 타입 매칭 |

---

## 슬라이드 (`slides/`)

| 파일 | 원본 | 생성일 |
|------|------|--------|
| [git-slide.html](./slides/git-slide.html) | [topics/git-basics.md](./topics/git-basics.md) | 2026-07-23 |
