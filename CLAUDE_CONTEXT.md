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

### 3-2. Farmers Market 보완전략 — P0/P1 이미 해결, P2-7만 남음
`document/MINS_Farmers_Market_보완전략.md`는 다른 PC에서 작성된 구(舊) 리포트(백엔드 붙기 전 목데이터 단계 기준)이며, 실제 코드와 대조해 이미 최신 상태로 갱신해둠. **남은 실제 작업은 P2-7(네비게이션 시맨틱 교체)뿐**:
- 대상: `HomePage.jsx`, `ProductListPage.jsx`, `ProductDetailPage.jsx`, `StoryPage.jsx`, `CartPage.jsx`, `OrderDetailPage.jsx`, `Footer.jsx`
- `<div>`/`<span onClick={...}>` → `<Link>`/`NavLink` 교체
- 설계 포인트: 상품 카드처럼 내부에 `<button>`(장바구니 담기)이 있으면 `<Link>`로 전체를 감쌀 때 `<button>`이 `<a>` 안에 중첩되어 HTML 스펙 위반 → `<article>` + 부분 `<Link>`, 또는 버튼에 `preventDefault`+`stopPropagation` 중 택1 필요

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

### 3-5. 배포 검증 결함 처리 — DEF-7·8·9·11만 남음 (2026-08-02)

Azure 배포 환경을 실제로 훑은 1회차 검증(`document/2026-08-02_배포검증_체크리스트.md`)에서 결함 11건이 나왔고, **P0·P1은 전부 해결·재검증까지 끝났다**. 남은 4건은 모두 P2다.

| ID | 남은 결함 | 레이어 |
|---|---|---|
| DEF-7 | CartPage·AdminPage에 `<main>` 없음, 푸터 "둘러보기"·배송지 카드가 `cursor:pointer` div라 키보드 접근 불가 | 프론트 |
| DEF-8 | 모바일 390px에서 "검색" 버튼이 `검/색`으로 세로 분리 | 프론트 CSS |
| DEF-9 | 관리자 주문 목록 "다음" 버튼이 마지막 페이지에서 비활성화되지 않음 | 프론트 |
| DEF-11 | 업로드된 Blob 이미지의 Content-Type이 `application/octet-stream` (화면 렌더는 정상) | 백엔드 |

> **DEF-7은 아래 3-2의 P2-7과 사실상 같은 작업**이다 — `div onClick` → `<Link>`/`<button>` 교체라는 같은 뿌리라서, 한 번에 처리하면 두 항목이 동시에 닫힌다.

**검증 환경 메모**: 배포 사이트 검증은 브라우저 자동화가 필요하다. Playwright MCP(`npx -y @playwright/mcp@latest`)는 **패키지 최초 다운로드가 30초 연결 타임아웃을 넘겨 실패**할 수 있으니, 세션 시작 전에 그 명령을 한 번 돌려 npx 캐시를 데워두면 된다. MCP를 못 쓸 때는 npx 캐시의 `playwright-core`를 Node 스크립트에서 직접 import해 Chromium을 몰 수 있다(설치된 chromium 리비전과 playwright-core 버전이 맞아야 함).

**운영 업그레이드 3대 항목 진행 상황:** 관측성(Actuator 헬스체크 + traceId 로그/MDC)은 2026-07-29 1단계 완료(`43f5d6f`). 데이터 안전성(`ddl-auto=update` → Flyway 전환)은 지금 규모에서는 과설계라고 판단해 보류 결정(진행 안 함). **남은 건 성능(N+1·인덱스)뿐.** Hook(커밋 전 포맷 강제)도 미구축(하네스 Layer 3, 우선순위 낮음).

## 4. 새 PC / 새 작업자가 이어받을 때 체크리스트

1. `git pull origin master`
2. `docker compose up -d` → `backend/ ./gradlew bootRun` → `frontend/ npm run dev` (상세는 `CLAUDE.md` "개발 환경 재설정" 참고)
   - Docker로 백엔드를 띄우는 경우 루트에 `.env`가 없으면 `TOSS_SECRET_KEY`가 빈 값으로 주입되어 가상계좌 결제가 "취소됨"으로 표시됨 — `.env.example`을 복사해 실제 토스 테스트 키를 채울 것 (3-1 참고)
3. 이 문서의 3번 섹션에서 이어할 작업 선택 (우선순위 낮은 순: 3-2 P2-7 < 3-3 성능 개선)
4. Claude에게 "CLAUDE_CONTEXT.md 읽고 [작업명] 이어서 해줘"라고 지시하면 됨

## 5. 이 문서 유지보수 원칙

- **세션 종료 시점에, 의미 있는 진행 상황 변화가 있었다면 이 문서를 갱신**한다 (todo 완료 처리, 새 보류 작업 추가 등).
- 사소한 버그 수정·리팩토링까지 전부 기록하지 않는다 — 그건 git 커밋 로그가 이미 하는 일이다. 이 문서는 **"다음 사람이 반드시 알아야 할 것"**만 담는다.
- 커밋 메시지 컨벤션에 맞춰 이 문서 갱신도 `docs:` 접두사로 커밋한다.
