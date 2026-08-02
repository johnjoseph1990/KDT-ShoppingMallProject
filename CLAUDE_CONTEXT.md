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

## 2. 현재 상태 (2026-08-01 기준)

- **로컬 `master`와 `origin/master`가 완전히 동기화됨**. 별도로 push할 것 없음.
- 2026-08-01, 다른 PC에서 작업한 8개 커밋(`97442f7..86f4b51`)을 pull로 반영함 — Azure Blob Storage 이미지 업로드 + Azure Container Apps 배포/CD 파이프라인. 상세는 3-4 참고.
- `git status`에 계속 뜨는 untracked 파일(`document/*.pdf`, `curriculum/`, `document/*.md` 학습노트 등)은 **의도적으로 커밋 안 하는 로컬 학습자료**임 — 삭제하거나 커밋 대상으로 제안할 필요 없음. 단, 인수인계용 학습노트는 예외적으로 커밋하기도 함(아래 6번 참고).

## 3. 진행 중 / 보류 작업

### 3-1. 토스페이먼츠 연동 — ✅ 완료 (2026-07-29/30)
결제창형·가상계좌(무통장입금) 연동 모두 완료. **이 PC에 루트 `.env`가 없어서 가상계좌 결제가 "취소됨"으로 뜨던 문제를 2026-07-29에 해결함**: 토스 개발자센터에서 발급받은 테스트 키(`test_ck_...`/`test_sk_...`)를 루트 `.env` + `frontend/.env`에 채운 뒤 `docker compose up -d --build`로 컨테이너 재생성, `docker exec mins-backend printenv TOSS_SECRET_KEY`로 주입 확인, Playwright로 로그인→주문→가상계좌(농협) 선택→결제까지 실행해 "입금대기" 상태 전환과 계좌번호 표시까지 검증 완료.
- 백엔드는 컨테이너로 띄울 경우 `docker-compose.yml`이 루트 `.env`를 자동으로 읽어 `TOSS_SECRET_KEY`에 주입함(다른 PC에서 새로 세팅할 때도 `bootRun` 셸 환경변수 대신 이 방식이 표준)
- 프론트 `frontend/.env`의 `VITE_TOSS_CLIENT_KEY`는 git 미포함(`.gitignore`) — 새 PC마다 `.env.example` 복사 후 채워야 함
- 카드결제 실제 승인까지는 2026-07-28에 이미 확인됨 (신한/롯데 등 실카드번호 입력은 PG 보안모듈로 자동화 불가하나, 사람이 직접 확인 완료)
- **남은 작업 없음.**

### 3-2. Farmers Market 보완전략 — ✅ 전 항목 완료 (2026-08-02)
`document/MINS_Farmers_Market_보완전략.md`는 다른 PC에서 작성된 구(舊) 리포트(백엔드 붙기 전 목데이터 단계 기준)이며, 실제 코드와 대조해 이미 최신 상태로 갱신해둠. 마지막까지 남아 있던 **P2-7(네비게이션 시맨틱 교체)도 2026-08-02에 완료**(커밋 `461dbf2`) — 배포 검증의 DEF-7과 같은 뿌리라 한 번에 처리했다.
- 미리 적어뒀던 설계 함정("카드 안에 `<button>`이 있으면 `<Link>`로 전체를 감쌀 수 없다")이 실제로 그대로 발생했고, **stretched link**(상품명만 `<a>` + `::after`를 카드 전체로 확장)로 해결했다. 상세는 `document/2026-08-02_배포검증_체크리스트.md`의 "DEF-7 상세" 참고.
- **남은 작업 없음.**

### 3-3. 서브에이전트 설계 — ✅ 6종 완성 (2026-07-25)
`.claude/agents/`: `spec-reviewer`, `security-reviewer`, `code-reviewer`, `learning-annotator`, `test-writer`, `reliability-reviewer`(신규) 모두 완성. **미완 TODO(human) 없음.**
- 목표 상향("부트캠프 제출물 → 실서비스 운영 수준")에 맞춰 `reliability-reviewer` 신규 추가 — 결제·주문의 멱등성/트랜잭션 경계/부분 실패/재시도/동시성 검토. 게이팅: "돈 걸린 결제·주문 🔴만 무조건 차단".
- `.gitignore`를 `.claude/*` + `!.claude/agents/`로 바꿔 에이전트 6종을 git 추적 등록(개인 설정 `settings.local.json`은 계속 무시).
- 배치 전략: 읽기전용 5종 병렬 + 쓰기 권한 `test-writer`만 단독 선행. 변경 규모별로 조합 스케일.
- 설계 회고·발표 메모: `document/2026-07-25_에이전트_설계_회고.md` (뼈대만, 나중에 확장 예정).

### 3-4. Azure Container Apps 배포 + Blob Storage 이미지 업로드 — ✅ 완료 (2026-08-01 이 PC에서 pull로 반영 확인)
다른 PC에서 진행되어 8개 커밋으로 pull된 작업. 두 갈래로 나뉜다.
- **이미지 업로드**: `AzureStorageConfig`/`AzureBlobService`/`UploadController`(`POST /api/admin/upload`) 추가. `AZURE_STORAGE_CONNECTION_STRING` 환경변수가 없으면 관련 빈을 아예 생성하지 않아(`@ConditionalOnExpression`/`@ConditionalOnBean`) 로컬 개발 시 이 값이 없어도 앱은 정상 기동됨(업로드 호출 시에만 503).
- **Azure Container Apps 배포**: `azure/setup.sh`로 ACR + Container Apps Environment + PostgreSQL Flexible Server 생성. `.github/workflows/ci.yml`에 `deploy` job 추가되어 **master push 시 테스트 통과 후 자동으로 Azure에 배포**됨(빌드→ACR push→Container Apps 이미지 업데이트). `nginx.conf`는 `BACKEND_URL`을 컨테이너 기동 시 런타임 주입받도록 바뀌어 로컬 Docker Compose와 Azure 양쪽에서 같은 이미지를 그대로 사용.
- `DevDataInitializer`가 `@Profile("dev")` → `@Profile("!test")`로 바뀌어, 배포된 prod 환경(Azure)에도 관리자/상품 시드 데이터가 들어감(배포 직후 빈 화면 방지).
- **남은 작업 없음.** 새 PC에서 Azure Blob Storage를 로컬 테스트하려면 `.env.example`의 `AZURE_STORAGE_CONNECTION_STRING`/`AZURE_STORAGE_CONTAINER_NAME`을 채우면 되고, 안 채워도 로컬 개발엔 지장 없음.

### 3-5. 배포 검증 결함 처리 — 12건 전부 종결 (2026-08-02)

Azure 배포 환경을 실제로 훑은 1회차 검증(`document/2026-08-02_배포검증_체크리스트.md`)에서 결함 12건이 나왔고, **12건 전부 해결·배포 후 재검증까지 끝났다. 남은 결함 없음.**

> DEF-7(시맨틱/접근성)은 2026-08-02에 위 3-2의 P2-7과 함께 종결됐다(커밋 `461dbf2`).
> DEF-8(모바일 버튼 줄바꿈)도 같은 날 종결. **한글은 글자 사이 어디서나 줄바꿈되므로 flex 아이템의 최소 너비가 한 글자가 된다** — 좁은 화면에 놓이는 한글 버튼에는 `flexShrink: 0` + `whiteSpace: 'nowrap'`을 습관적으로 붙일 것.
> DEF-9(관리자 페이징)도 같은 날 종결(커밋 `c4c8143`). **백엔드 Spring `Page` 응답에는 `totalPages`·`last`·`totalElements`가 들어 있는데 프론트가 `content`만 꺼내 쓰고 버리는 패턴을 경계할 것** — 그러면 "마지막 페이지인지"를 알 수 없어 `items.length === 0` 같은 한 칸 늦은 조건을 쓰게 된다.
> DEF-12(추천 상품 가짜 링크)도 같은 날 종결(커밋 `db5f396`).
> DEF-11(Blob Content-Type)도 같은 날 종결(커밋 `3bab244`). **결함을 고치는 수정이 새 위험을 만들 수 있다** — Content-Type을 클라이언트가 보낸 값 그대로 붙이면 공개 컨테이너에 `text/html`을 심을 수 있게 되므로, 화이트리스트 검사를 같이 넣었다. 프론트에 같은 검사가 있어도 `curl`로 API를 직접 부르면 건너뛰므로 **서버 검사가 진짜 방어선**이다. 또 하나: **Content-Type은 업로드 시점에 새겨지는 메타데이터라 코드 수정이 소급되지 않는다** — 이미 올라간 파일은 백필이 따로 필요하다.
>
> **테스트 관련 두 가지 교훈** (DEF-11 작업 중 CI가 DEF-9 테스트에서 실패해 드러남, 커밋 `e08d761`):
> ① **표 머리글은 로딩 완료 신호가 아니다.** `waitFor(제목이 뜰 때까지)` 뒤에 단언하면, 머리글은 데이터와 무관하게 즉시 렌더되므로 응답 반영 전에 검사가 돈다. 로컬에서는 통과하고 CI에서만 깨지는 전형적인 경쟁 조건. `await act(async () => {})`로 대기 중인 프로미스와 리렌더를 흘려보낸 뒤 판정한다.
> ② **"결과 0건" 테스트는 `waitFor(...toBeDisabled())`로 쓰면 헛통과한다** — 응답 전 초기 상태가 이미 disabled라 즉시 통과해버려 고장난 코드도 못 잡는다. 의심되면 **구현을 일부러 되돌려(변이 테스트) 테스트가 실제로 실패하는지 확인**할 것.
>
> **이 프로젝트에서 반복된 단일 원인 — 복제된 코드**: DEF-6(상태 라벨), DEF-8(초기화 버튼), DEF-9(두 패널의 페이징), DEF-12(추천 카드)가 모두 "같은 코드가 두 군데 있어 한쪽만 고쳐졌다"였다. DEF-12에서는 **DEF-4의 회색 빈 칸 결함까지 같은 복제본에 남아 있었다** — 한 복제본이 두 개의 수정을 동시에 놓친 것이다.
> 실천 규칙 두 가지: ① 카드·목록·페이징 같은 UI를 손으로 다시 그리지 말고 기존 컴포넌트를 재사용한다. ② **회귀 테스트를 컴포넌트에만 걸면 복제본은 안 지켜진다** — DEF-12가 정확히 그렇게 새어나갔으므로, 화면 단위 테스트(`ProductDetailPage.test.jsx`·`AdminPage.test.jsx`)를 함께 둔다.

**검증 환경 메모**: 배포 사이트 검증은 브라우저 자동화가 필요하다. Playwright MCP(`npx -y @playwright/mcp@latest`)는 **패키지 최초 다운로드가 30초 연결 타임아웃을 넘겨 실패**할 수 있으니, 세션 시작 전에 그 명령을 한 번 돌려 npx 캐시를 데워두면 된다. MCP를 못 쓸 때는 npx 캐시의 `playwright-core`를 Node 스크립트에서 직접 import해 Chromium을 몰 수 있다(설치된 chromium 리비전과 playwright-core 버전이 맞아야 함).

**운영 업그레이드 3대 항목 진행 상황:** 관측성(Actuator 헬스체크 + traceId 로그/MDC)은 2026-07-29 1단계 완료(`43f5d6f`). 데이터 안전성(`ddl-auto=update` → Flyway 전환)은 지금 규모에서는 과설계라고 판단해 보류 결정(진행 안 함). **남은 건 성능(N+1·인덱스)뿐.** Hook(커밋 전 포맷 강제)도 미구축(하네스 Layer 3, 우선순위 낮음).

### 3-6. 다음 세션 착수 목록 (2026-08-02 마감 시점)

배포 검증 결함은 전부 끝났고, DEF-11 작업 중에 부수적으로 발견한 **미처리 항목 3개**가 남았다. 셋 다 급하지 않다.

| # | 항목 | 위치 | 판단 |
|---|---|---|---|
| 1 | **`ProductListPage`의 페이저가 페이지 크기 10을 하드코딩** — `disabled={(page + 1) * 10 >= totalElements}`. 결과는 맞지만 진짜 출처는 백엔드 `@PageableDefault(size = 10)`이라, 서버에서 20으로 바꾸면 **화면만 조용히 틀려진다** | `frontend/src/pages/ProductListPage.jsx:308` | DEF-9와 같은 종류의 잠재 결함. 관리자 페이저처럼 `totalPages`를 쓰면 프론트가 페이지 크기를 알 필요가 없어진다 |
| 2 | **떠 있는 미완성 파일 `frontend/src/utils/pagination.js`** — DEF-9 때 만들다 만 것으로 `isLastPage`가 `TODO(human)`인 채 비어 있고 **아무 데서도 import되지 않는다**(실제 수정은 `AdminPage.jsx`의 `Pager`로 들어갔다). untracked라 CI·빌드에는 영향 없음 | 같은 경로 | 1번을 할 때 이 파일을 완성해 두 페이저가 함께 쓰게 하거나, 안 쓸 거면 삭제 |
| 3 | **DEF-11의 `security-reviewer` 검토 미실시** | — | 관리자 전용 엔드포인트의 입력 검증(허용 목록)을 건드렸으므로 `CLAUDE.md` 규칙상 검토 대상에 가깝다. 화이트리스트 방식 자체는 표준적이라 위험도는 낮음 |

**그 다음 큰 작업**: 성능 개선(N+1·인덱스) — 위 3-5 끝의 "운영 업그레이드 3대 항목" 참조.

## 4. 새 PC / 새 작업자가 이어받을 때 체크리스트

1. `git pull origin master`
2. `docker compose up -d` → `backend/ ./gradlew bootRun` → `frontend/ npm run dev` (상세는 `CLAUDE.md` "개발 환경 재설정" 참고)
   - Docker로 백엔드를 띄우는 경우 루트에 `.env`가 없으면 `TOSS_SECRET_KEY`가 빈 값으로 주입되어 가상계좌 결제가 "취소됨"으로 표시됨 — `.env.example`을 복사해 실제 토스 테스트 키를 채울 것 (3-1 참고)
3. 이 문서의 3번 섹션에서 이어할 작업 선택 (**배포 검증 결함은 12건 전부 종결. 다음 후보는 성능 개선(N+1·인덱스)**)
4. Claude에게 "CLAUDE_CONTEXT.md 읽고 [작업명] 이어서 해줘"라고 지시하면 됨

## 5. 이 문서 유지보수 원칙

- **세션 종료 시점에, 의미 있는 진행 상황 변화가 있었다면 이 문서를 갱신**한다 (todo 완료 처리, 새 보류 작업 추가 등).
- 사소한 버그 수정·리팩토링까지 전부 기록하지 않는다 — 그건 git 커밋 로그가 이미 하는 일이다. 이 문서는 **"다음 사람이 반드시 알아야 할 것"**만 담는다.
- 커밋 메시지 컨벤션에 맞춰 이 문서 갱신도 `docs:` 접두사로 커밋한다.
