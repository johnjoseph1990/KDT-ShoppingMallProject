# CLAUDE_CONTEXT — 작업 인수인계 문서

> **이 문서의 목적**: `CLAUDE.md`가 "변하지 않는 규칙(컨벤션, 정책)"을 담는다면, 이 문서는 "지금 뭘 하고 있었는지 / 뭐가 남았는지"를 담는다.
> 다른 PC나 다른 작업자가 이 저장소를 처음 열었을 때, Claude에게 "CLAUDE_CONTEXT.md 읽고 이어서 작업해줘"라고 하면 바로 맥락을 잡을 수 있게 하는 것이 목표다.
> **git에 커밋되어야** 다른 PC에서도 보인다 — Claude의 개인 메모리(`~/.claude/.../memory/`)는 이 PC·이 계정에만 있고 다른 작업자에게는 보이지 않기 때문에, 이 문서로 그 간극을 메운다.

---

## 1. 프로젝트 한눈에 보기

KDT 교육과정 쇼핑몰 프로젝트 (Spring Boot + React). 상세 컨벤션·환경 설정은 `CLAUDE.md` 참고.

**구현된 주요 도메인** (`backend/src/main/java/.../domain/`):
- `member` — 회원가입/로그인/탈퇴, Spring Security 기반 인증
- `product` / `review` — 상품, 리뷰(수정 API 포함), 리뷰 기반 키워드 추천
- `cart` / `order` / `payment` — 장바구니 → 주문 생성(낙관적 락 + `@Retryable`) → 토스페이먼츠 결제
- `address` — 배송지 관리

## 2. 현재 상태 (2026-07-25 기준)

- **로컬이 origin보다 1커밋 앞서 있음** (`e830af8`, viewCount DEFAULT 0 수정) — 아직 `push` 안 됨. 다른 PC에서 이어받으려면 이 커밋을 먼저 push해야 함.
- `git status`에 계속 뜨는 untracked 파일(`document/*.pdf`, `curriculum/`, `document/*.md` 학습노트 등)은 **의도적으로 커밋 안 하는 로컬 학습자료**임 — 삭제하거나 커밋 대상으로 제안할 필요 없음. 단, 인수인계용 학습노트는 예외적으로 커밋하기도 함(아래 6번 참고).

## 3. 진행 중 / 보류 작업

### 3-1. 토스페이먼츠 연동 — 거의 완료, 선택적 확인만 남음
결제창 오픈까지 Playwright로 검증 완료(상품명·금액·클라이언트 키 정상). 카드사 선택 이후 실제 카드결제 성공/실패 리다이렉트는 PG가 자동화 클릭을 막아 **사람이 브라우저에서 직접** 테스트 카드(`4330000000000004`)로 확인해야 함. 급하지 않은 선택 작업.
- 백엔드 `TOSS_SECRET_KEY`는 OS 환경변수로만 주입 (`bootRun` 실행 셸마다 `$env:TOSS_SECRET_KEY` 지정 필요, 파일 저장 안 함)
- 프론트 `frontend/.env`의 `VITE_TOSS_CLIENT_KEY`는 git 미포함(`.gitignore`)

### 3-2. Farmers Market 보완전략 — P0/P1 이미 해결, P2-7만 남음
`document/MINS_Farmers_Market_보완전략.md`는 다른 PC에서 작성된 구(舊) 리포트(백엔드 붙기 전 목데이터 단계 기준)이며, 실제 코드와 대조해 이미 최신 상태로 갱신해둠. **남은 실제 작업은 P2-7(네비게이션 시맨틱 교체)뿐**:
- 대상: `HomePage.jsx`, `ProductListPage.jsx`, `ProductDetailPage.jsx`, `StoryPage.jsx`, `CartPage.jsx`, `OrderDetailPage.jsx`, `Footer.jsx`
- `<div>`/`<span onClick={...}>` → `<Link>`/`NavLink` 교체
- 설계 포인트: 상품 카드처럼 내부에 `<button>`(장바구니 담기)이 있으면 `<Link>`로 전체를 감쌀 때 `<button>`이 `<a>` 안에 중첩되어 HTML 스펙 위반 → `<article>` + 부분 `<Link>`, 또는 버튼에 `preventDefault`+`stopPropagation` 중 택1 필요

### 3-3. 서브에이전트 설계 — 5개 중 4개 완성, 1개 TODO(human) 미완성
`.claude/agents/`: `spec-reviewer`, `security-reviewer`, `code-reviewer`, `learning-annotator`는 완성. **`test-writer.md`에 TODO(human) 미완성** — "테스트 실패 시 대응 정책"(프로덕션 코드 수정 허용 범위, 재시도 횟수, 원인 불명확 시 보고 방식)을 아직 채워야 함.
- Hook(커밋 전 포맷 강제)은 아직 미구축 (하네스 Layer 3, 남은 작업)

## 4. 새 PC / 새 작업자가 이어받을 때 체크리스트

1. `git pull origin master` (2번 항목의 미push 커밋이 이미 push되어 있다면 문제 없음)
2. `docker compose up -d` → `backend/ ./gradlew bootRun` → `frontend/ npm run dev` (상세는 `CLAUDE.md` "개발 환경 재설정" 참고)
3. 이 문서의 3번 섹션에서 이어할 작업 선택 (우선순위 낮은 순: 3-1 선택적 확인 < 3-2 P2-7 < 3-3 test-writer TODO)
4. Claude에게 "CLAUDE_CONTEXT.md 읽고 [작업명] 이어서 해줘"라고 지시하면 됨

## 5. 이 문서 유지보수 원칙

- **세션 종료 시점에, 의미 있는 진행 상황 변화가 있었다면 이 문서를 갱신**한다 (todo 완료 처리, 새 보류 작업 추가 등).
- 사소한 버그 수정·리팩토링까지 전부 기록하지 않는다 — 그건 git 커밋 로그가 이미 하는 일이다. 이 문서는 **"다음 사람이 반드시 알아야 할 것"**만 담는다.
- 커밋 메시지 컨벤션에 맞춰 이 문서 갱신도 `docs:` 접두사로 커밋한다.
