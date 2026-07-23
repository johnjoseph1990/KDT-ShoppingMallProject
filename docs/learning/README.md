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
└── slides/            ← (미추적) 발표 확정 시에만 수동 생성, git 제외
```

**운영 규칙 (2026-07-23 개정 — 저비용 학습 기록 방침)**

학습 기록은 별도 문서가 아니라 **코드에 통합된 저비용 채널**로 남긴다:

1. **코드 주석** — 개념·"왜"를 코드 옆에 (CLAUDE.md "코드 주석 규칙" 참조)
2. **커밋 메시지 본문 3~5줄** — 그날 배운 것, `git log --grep`으로 검색
3. **한글 테스트 메서드명** — 동작 명세 역할

- 신규 `notes/`·`slides/`는 **작성하지 않는다** (슬라이드는 git 미추적, 발표 시에만 수동 생성)
- `CONCEPT_INDEX.md`는 **동결** (더 이상 갱신하지 않음)
- 아래 `topics/`·`notes/`·`slides/` 표는 **과거 산출물 기록**으로 보존한다 (삭제하지 않음)

---

## 주제별 정리 (`topics/`)

### Git / 협업 도구

| 문서 | 핵심 키워드 | 최종 수정 |
|------|-----------|----------|
| [Git 기초](./topics/git-basics.md) | commit, push, pull, clone, conflict, staging | 2026-07-23 |

### Spring Boot 백엔드

| 문서 | 핵심 키워드 | 최종 수정 |
|------|-----------|----------|
| [Spring 핵심 개념](./topics/spring-concepts.md) | DI, 생성자 주입, 전략 패턴, @Transactional, @Version, Spring Security | 2026-07-23 |
| [테스트 계층 구조](./topics/spring-test-layers.md) | @WebMvcTest, @ExtendWith, Mockito, 결정론적 테스트, @WithMockUser, 401 vs 403 | 2026-07-23 |
| [재고 동기화 설계](./topics/stock-sync.md) | 낙관적 락, @Version, restoreStock, @DataJpaTest, 3계층 테스트 | 2026-07-23 |
| [관리자 기능 구현](./topics/admin-feature.md) | Spring Security URL 인가, 401 vs 403, @WithMockUser, PrivateRoute adminOnly, 조건부 스프레드 | 2026-07-23 |
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
| 2026-07-23 | [관리자 기능 구현](./notes/2026-07-23_관리자기능_학습노트.md) | 401 vs 403, @WithMockUser, USER권한 403 누락 발견, 상태 필터 UI |

---

## 슬라이드 (`slides/`)

| 파일 | 원본 | 생성일 |
|------|------|--------|
| [git-slide.html](./slides/git-slide.html) | [topics/git-basics.md](./topics/git-basics.md) | 2026-07-23 |
| [spring-concepts-slide.html](./slides/spring-concepts-slide.html) | [topics/spring-concepts.md](./topics/spring-concepts.md) | 2026-07-23 |
| [spring-test-layers-slide.html](./slides/spring-test-layers-slide.html) | [topics/spring-test-layers.md](./topics/spring-test-layers.md) | 2026-07-23 |
| [stock-sync-slide.html](./slides/stock-sync-slide.html) | [topics/stock-sync.md](./topics/stock-sync.md) | 2026-07-23 |
| [address-management-slide.html](./slides/address-management-slide.html) | [topics/address-management.md](./topics/address-management.md) | 2026-07-23 |
| [frontend-mypage-slide.html](./slides/frontend-mypage-slide.html) | [topics/frontend-mypage-redesign.md](./topics/frontend-mypage-redesign.md) | 2026-07-23 |
