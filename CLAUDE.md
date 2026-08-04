# KDT ShoppingMall Project — Claude Code 가이드

## 언어 및 커뮤니케이션

- 답변은 항상 **한국어**로 작성
- 대화창 설명은 간결하게 — 단, 코드 설명을 요청받았을 때는
  "하는 일 한 문장 → 핵심 개념(Java/Spring 문법) → 실행 흐름 순서 → 자주 하는 실수 1가지" 순으로 다룬다

## 코드 주석 규칙 (학습용, 필수)

이 프로젝트는 학습이 목적이므로, 코드 내 주석은 **적극적으로 작성**한다 (일반적인 "주석은 최소화" 원칙의 예외):

- 새로 작성하거나 수정하는 코드는 핵심 라인마다 한국어 주석으로 "무엇을/왜" 설명
- 클래스/인터페이스가 처음 등장하면, 그 위에 이 개념이 무엇이고 왜 쓰이는지 1~2줄 주석 추가 (예: `@Service`가 왜 붙는지, `JpaRepository`를 상속하면 뭐가 되는지)
- Spring 어노테이션이 처음 등장하면 "왜 붙이는지"를 주석으로 한 문장 설명
- 기존 코드를 수정할 때는 그 파일에 주석이 없다면 이번 기회에 같이 보강
- 예외: 포맷터(Spotless/Prettier)가 자동 생성하는 코드, 테스트의 반복적인 given-when-then 블록처럼 자명한 부분까지 전부 달 필요는 없음

## 재현성 원칙 (필수) — "git에 없으면 없는 것"

상태 변화는 손이 아니라 **재현 가능한 코드**로 남긴다. 클론 → `docker compose up -d` → `bootRun`만 하면 누구 PC에서나 동일한 화면/데이터가 떠야 한다.

**단, 모든 데이터를 시드로 만들면 오히려 과설계다. 아래 경계선으로 구분한다.**

### 시드 대상 경계선 (중요)

**판단 기준(한 문장):** "이게 없으면 화면·기능을 *시연*할 수 없나?" → 예면 **기준 데이터(시드 대상)**, "사용자가 앱을 쓰면서 *만들어내는* 결과인가?" → 예면 **런타임 데이터(시드 금지)**.

| 구분 | 엔티티 | 처리 |
|---|---|---|
| ✅ 시드 대상 (기준 데이터) | `Product`, `ProductTag`, 관리자·데모 계정(`Member` 중 `admin@`/`test@`/`user01~10`) | `DevDataInitializer`에 멱등하게 |
| ❌ 시드 금지 (런타임 데이터) | `Order`, `OrderItem`, `Payment`, `CartItem`, `Review`, `Address`, 실제 가입한 일반 `Member` | 시드하지 않는다. 필요한 테스트 시나리오는 **테스트 코드 안에서** 생성 |

런타임 데이터를 시드에 박으면 "사용자가 아무 행동 안 했는데 주문·리뷰가 존재하는" 비현실적 상태가 되고, 유지보수 부담만 커진다. 시연에 꼭 그런 데이터가 필요하면 시드가 아니라 **재현 스크립트/테스트 픽스처**로 분리한다.

- **DB 데이터를 관리자 UI나 콘솔로 직접 넣지 말 것.** 위 표의 ✅ 기준 데이터는 시드 코드(`DevDataInitializer`)에 넣는다. 관리자 UI로 넣은 데이터는 git에 안 남아 클론/재시작 시 사라진다.
- **시드는 멱등하게(idempotent) 작성한다.** 넣기 전에 `existsBy...`로 중복을 확인해, 몇 번 실행해도 중복이 쌓이지 않게 한다. `count() > 0` 같은 전체 가드는 "새 데이터를 아예 못 넣는" 함정이 있으니 항목별로 체크한다.
- **커밋 메시지 = 그 커밋의 diff.** diff에 없는 작업(수동 DB 조작 등)을 메시지에 적지 않는다. 코드 밖에서 한 작업이 꼭 필요하면 시드/마이그레이션으로 옮기거나 커밋 본문에 재현 절차를 남긴다.
- 데이터·스키마 변경 이력 관리가 필요해지면 Flyway 마이그레이션(`V1__`, `V2__`...) 도입을 검토한다.

## GitHub 코드 관리 (필수)

원격 저장소: `https://github.com/johnjoseph1990/KDT-ShoppingMallProject.git`

**다중 PC 작업 규칙 (이 프로젝트 고유 제약):**
- 새 PC에서 작업 시작 전 반드시 `git pull origin master` 먼저 실행
- 작업 종료 시 반드시 `git push origin master` 완료 후 자리 이동
- 충돌 발생 시 직접 해결 후 커밋

## 개발 환경 재설정 (다른 PC에서 클론 시)

```bash
# 1. 클론
git clone https://github.com/johnjoseph1990/KDT-ShoppingMallProject.git
cd KDT-ShoppingMallProject

# 1-1. pre-commit 포맷터 훅 활성화 (PC마다 한 번만 실행)
git config core.hooksPath .githooks

# 2. DB 실행 (Docker 필요) — application.properties 기본값이 PostgreSQL(localhost:5433)이므로
#    이 컨테이너 없이 바로 bootRun 하면 연결 실패로 뜨지 않는다
docker compose up -d

# 3. 백엔드 실행 (Java 17 필요)
cd backend
./gradlew bootRun

# 4. 프론트엔드 실행
cd frontend
npm install
npm run dev
```

## 커밋 메시지 컨벤션

```
Day N: 기능 요약
feat: 새 기능
fix: 버그 수정
refactor: 리팩토링
docs: 문서
```

## TDD + 코드 품질 (필수)

**개발 프로세스 (현실화):** "테스트 선행(TDD)"을 구호로만 두지 않는다. 지킬 수 있고 커밋
그래프로 검증되는 규칙으로 명문화한다.

- **회귀·단위 테스트 필수:** 새 기능(`feat:`)에는 대응 테스트가 반드시 따른다. 가능하면
  실패 테스트를 먼저 쓰고(TDD), 최소한 같은 커밋이나 직후 `test:` 커밋으로 남긴다.
- **검증 가능성:** 리뷰 때 `git log`에서 `feat:` 커밋에 대응 테스트가 비어 있으면 그 기능은
  "미완"으로 본다. (과거 `feat` 56 : `test` 7의 간극은 테스트가 feat에 섞였거나 사후로 밀린
  신호였다 — 그래서 "선행 구호" 대신 "필수 + 검증 가능"으로 현실화했다.)

```
① 계획 공유 (비자명한 기능만; 사소한 수정은 생략, 1~2줄 승인 후 진행)
② 테스트 작성 — 백엔드는 실패 테스트 선행. 프론트엔드는 vitest+Testing Library 단위 테스트,
   핵심 사용자 경로는 Playwright E2E 스모크(frontend/e2e/)로 검증
③ 구현 코드 작성
④ 테스트 통과 확인 (백: ./gradlew check / 프: npm run test:run, 경로 변경 시 npm run e2e)
⑤ spotlessApply / npm run format
⑥ 커밋 메시지 제안
```

> **학습 기록은 저비용 채널로 유지한다.** 학습을 없애지 않고, 부담이 적은 채널로 남긴다:
> - **코드 주석** — 개념·"왜"를 코드 옆에 (위 "코드 주석 규칙" 참조)
> - **커밋 메시지 본문 3~5줄** — 그날 배운 것 기록, `git log --grep`으로 검색
> - **한글 테스트 메서드명** — 동작 명세 역할
> - **`docs/learning/notes/` 날짜별 학습노트** — 위 세 채널로 담기 어려운 조사·회고성 기록은
>   여기에 남긴다(공식 채널로 인정). 발표 슬라이드(`docs/learning/slides/`)는 발표 확정 시에만.

**테스트 계층 구조:**
- `service/` — `@ExtendWith(MockitoExtension.class)` 단위 테스트 (Mockito Mock)
- `repository/` — `@DataJpaTest` 통합 테스트 (실제 H2 DB)
- `controller/` — `@WebMvcTest` + `@Import(SecurityConfig.class)` 슬라이스 테스트

**커버리지 목표:** 서비스 레이어 80% 이상 (JaCoCo 설정: 전체 60% 최소)

**E2E 스모크 (프론트, 로컬):** 핵심 사용자 경로 5개(홈·목록→상세·회원가입·장바구니·관리자)는
`frontend/e2e/`의 Playwright 스모크로 검증한다. 단위 테스트(하단)만으로는 못 잡는 통합·화면·인가
결함을 배포 전에 거른다. 실행법·경계는 `frontend/e2e/README.md` 참고. (CI 자동화는 전체 스택을
러너에 띄워야 해 아직 로컬 전용 — 배포 전 수동으로 `npm run e2e`를 돌린다.)

**결함(DEF) 처리 절차 (필수):** 버그를 고칠 때 "고쳤다"로 끝내지 않는다.
1. 그 버그를 **재현하는 실패 테스트**를 먼저 만든다(단위/슬라이스, 화면 결함이면 E2E 스모크).
2. 구현을 고쳐 그 테스트를 통과시킨다.
3. 테스트 통과 = 결함 종결. "배포 후 재검증" 문서는 보조일 뿐, 종결의 근거는 회귀 테스트다.

> 과거 DEF 16건 중 8건이 배포 후에야 발견된 건, 실패가 나는 지점(통합·화면)에 자동 검증이
> 없어서였다. 회귀 테스트를 종결 조건으로 강제하면 같은 결함이 두 번 배포되지 않는다.

## 코딩 컨벤션

**포맷터 (자동 적용, 수동 스타일 논쟁 금지):**
- 백엔드: Gradle Spotless + Google Java Format
- 프론트엔드: Prettier (`frontend/.prettierrc.json`)
- 커밋 전 반드시 `spotlessApply` / `format` 실행 후 diff 확인

**네이밍 규칙:**
- 클래스: `PascalCase` (예: `OrderService`, `ProductResponse`)
- 메서드/변수: `camelCase`, 불리언은 `is`/`has` 접두사 (예: `isOutOfStock`)
- DTO는 역할 접미사로 구분: `~Request`(입력), `~Response`(출력)
- 커스텀 예외는 `~Exception` 접미사, `GlobalExceptionHandler`에서 일괄 처리

**패키지 구조 (도메인 기준 분리):**
- `domain/{도메인명}` — 엔티티 + 해당 도메인 전용 enum (예: `domain/order/OrderStatus`)
- `controller`, `service`, `repository`, `dto/{도메인명}`, `exception` — 계층별 최상위 패키지

**커밋 메시지:** 위 "커밋 메시지 컨벤션" 참고

## 보안 테스트 (필수)

인증·인가·결제 관련 코드를 변경했으면 **`security-reviewer` 에이전트로 보안 검토를 받는다.**
OWASP TOP 10 체크리스트와 Spring Boot 체크포인트는 그 에이전트 정의(`.claude/agents/security-reviewer.md`)에
있으므로 여기에 중복해두지 않는다 — 기준을 바꿀 때는 그 파일을 수정한다.

## 민감 정보 주의

PostgreSQL 전환 이후 `application.properties`의 `spring.datasource.password` 기본값(`mins1234`)은 로컬 개발용 Docker 컨테이너(`docker-compose.yml`) 비밀번호로, 환경변수 `SPRING_DATASOURCE_PASSWORD`로 오버라이드 가능한 구조라 저장소에 커밋해도 문제없음. 단, 운영(prod) 배포 시에는 반드시 환경변수로 실제 비밀번호를 주입하고, 기본값을 그대로 쓰지 말 것.

## Skill routing

When the user's request matches an available skill, invoke it via the Skill tool. When in doubt, invoke the skill.

Key routing rules:
- Product ideas/brainstorming → invoke /office-hours
- Strategy/scope → invoke /plan-ceo-review
- Architecture → invoke /plan-eng-review
- Design system/plan review → invoke /design-consultation or /plan-design-review
- Full review pipeline → invoke /autoplan
- Bugs/errors → invoke /investigate
- QA/testing site behavior → invoke /qa or /qa-only
- Code review/diff check → invoke /review
- Visual polish → invoke /design-review
- Ship/deploy/PR → invoke /ship or /land-and-deploy
- Save progress → invoke /context-save
- Resume context → invoke /context-restore
- Author a backlog-ready spec/issue → invoke /spec
