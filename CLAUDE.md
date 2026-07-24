# KDT ShoppingMall Project — Claude Code 가이드

## 언어 및 커뮤니케이션

- 답변은 항상 **한국어**로 작성
- 코드 설명(대화창)은 간결하게
- 방금 작성한(또는 지정한) 코드를 초보자 관점에서 설명해줘:
1. 이 코드가 하는 일 (한 문장)
2. 핵심 개념 (Java/Spring 문법 포함)
3. 실행 흐름을 순서대로
4. 자주 하는 실수 1가지

## 코드 주석 규칙 (학습용, 필수)

이 프로젝트는 학습이 목적이므로, 코드 내 주석은 **적극적으로 작성**한다 (일반적인 "주석은 최소화" 원칙의 예외):

- 새로 작성하거나 수정하는 코드는 핵심 라인마다 한국어 주석으로 "무엇을/왜" 설명
- 클래스/인터페이스가 처음 등장하면, 그 위에 이 개념이 무엇이고 왜 쓰이는지 1~2줄 주석 추가 (예: `@Service`가 왜 붙는지, `JpaRepository`를 상속하면 뭐가 되는지)
- Spring 어노테이션이 처음 등장하면 "왜 붙이는지"를 주석으로 한 문장 설명
- 기존 코드를 수정할 때는 그 파일에 주석이 없다면 이번 기회에 같이 보강
- 예외: 포맷터(Spotless/Prettier)가 자동 생성하는 코드, 테스트의 반복적인 given-when-then 블록처럼 자명한 부분까지 전부 달 필요는 없음

## 개발 철학

- 불필요한 추상화나 기능 추가 금지 — 요청한 것만 구현
- 에러 핸들링은 시스템 경계(외부 입력, 외부 API)에서만 추가
- 보안 취약점(SQL 인젝션, XSS 등) 주의하여 코드 작성

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

## 프로젝트 개요
KDT 교육과정 쇼핑몰 프로젝트. Spring Boot(백엔드) + React(프론트엔드) 풀스택 구성.

- 백엔드: `backend/` — Spring Boot 3.5, Java 17, H2 인메모리 DB, Spring Security
- 프론트엔드: `frontend/` — React + Vite, `/api` → `http://localhost:8080` 프록시

## GitHub 코드 관리 (필수)

원격 저장소: `https://github.com/johnjoseph1990/KDT-ShoppingMallProject.git`

**작업 전 항상 pull:**
```
git pull origin master
```

**작업 후 commit + push:**
```
git add <파일>
git commit -m "설명"
git push origin master
```

**다중 PC 작업 규칙:**
- 새 PC에서 시작 전 반드시 `git pull` 먼저 실행
- 작업 종료 시 반드시 `git push` 완료 후 자리 이동
- 충돌 발생 시 직접 해결 후 커밋

## 개발 환경 재설정 (다른 PC에서 클론 시)

```bash
# 1. 클론
git clone https://github.com/johnjoseph1990/KDT-ShoppingMallProject.git

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

**개발 프로세스:** 테스트를 구현보다 먼저 작성한다 (테스트 선행 방식)

```
① 계획 공유 (비자명한 기능만; 사소한 수정은 생략, 1~2줄 승인 후 진행)
② 테스트 먼저 작성 (실패하는 테스트) — 백엔드 기준. 프론트엔드는 테스트 인프라가 없어 브라우저 수동 검증으로 대체
③ 구현 코드 작성
④ 테스트 실행 통과 확인
⑤ spotlessApply / npm run format
⑥ 커밋 메시지 제안
```

> **학습 기록은 저비용 채널로 유지한다.** 별도 학습 문서(notes/, slides/)는 만들지 않되, 학습을 없애는 게 아니라 코드에 통합된 채널로 옮긴다:
> - **코드 주석** — 개념·"왜"를 코드 옆에 (위 "코드 주석 규칙" 참조)
> - **커밋 메시지 본문 3~5줄** — 그날 배운 것 기록, `git log --grep`으로 검색
> - **한글 테스트 메서드명** — 동작 명세 역할

**테스트 계층 구조:**
- `service/` — `@ExtendWith(MockitoExtension.class)` 단위 테스트 (Mockito Mock)
- `repository/` — `@DataJpaTest` 통합 테스트 (실제 H2 DB)
- `controller/` — `@WebMvcTest` + `@Import(SecurityConfig.class)` 슬라이스 테스트

**커버리지 목표:** 서비스 레이어 80% 이상 (JaCoCo 설정: 전체 60% 최소)

**테스트 실행:**
```
./gradlew test                 # 테스트만
./gradlew test jacocoTestReport # 테스트 + 커버리지 리포트
```
커버리지 리포트 위치: `backend/build/reports/jacoco/test/html/index.html`

**CI/CD (GitHub Actions):**
- `master` 브랜치 push/PR 시 자동 실행
- 백엔드: Java 17 → Gradle test → JaCoCo 리포트
- 프론트엔드: Node 20 → npm ci → build 검증
- 실패 시 merge 불가

## 코딩 컨벤션

**포맷터 (자동 적용, 수동 스타일 논쟁 금지):**
- 백엔드: Gradle Spotless + Google Java Format
  ```
  cd backend
  ./gradlew spotlessApply   # 포맷 자동 적용
  ./gradlew spotlessCheck   # 포맷 위반 시 실패 (check 태스크에 포함되어 build 시 자동 검증)
  ```
- 프론트엔드: Prettier (`frontend/.prettierrc.json`, 세미콜론 없음·싱글쿼트·printWidth 100)
  ```
  cd frontend
  npm run format         # 포맷 자동 적용
  npm run format:check   # 포맷 위반 시 실패
  npm run lint           # oxlint 정적 분석 (포맷과 별개)
  ```
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

보안 테스트 진행 시 **OWASP TOP 10** 공격 방식을 중점적으로 체크할 것:

1. **A01 - 접근 제어 취약점**: 인증/인가 우회, URL 직접 접근, 권한 없는 API 호출
2. **A02 - 암호화 실패**: 민감 정보 평문 전송, 약한 해시 알고리즘 사용
3. **A03 - 인젝션**: SQL 인젝션, JPQL 인젝션, 파라미터 조작
4. **A04 - 안전하지 않은 설계**: 비즈니스 로직 결함, 입력값 검증 누락
5. **A05 - 보안 설정 오류**: 디버그 모드 노출, 불필요한 엔드포인트 오픈
6. **A06 - 취약한 컴포넌트**: 오래된 의존성 라이브러리 사용
7. **A07 - 인증 실패**: 무차별 대입 공격, 세션 고정, JWT 검증 미흡
8. **A08 - 소프트웨어 무결성 실패**: 신뢰할 수 없는 외부 리소스 사용
9. **A09 - 로깅/모니터링 부재**: 보안 이벤트 미기록, 비정상 접근 감지 불가
10. **A10 - SSRF**: 서버 측 요청 위조, 내부 네트워크 접근

**Spring Boot 프로젝트 주요 체크포인트:**
- Spring Security 설정 — 미인증 접근 가능한 엔드포인트 존재 여부
- `@PreAuthorize` / `@Secured` 누락 여부
- JPA 쿼리에서 파라미터 바인딩 미사용 여부
- JWT 토큰 만료/서명 검증 로직
- `application.properties` 민감 정보 하드코딩 여부

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
