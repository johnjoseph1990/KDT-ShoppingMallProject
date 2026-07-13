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

# 2. 백엔드 실행 (Java 17 필요)
cd backend
./gradlew bootRun

# 3. 프론트엔드 실행
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

**개발 프로세스:** 기능 구현 직후 반드시 테스트 작성 (테스트 병행 방식)

```
기능 설계 → 구현 → 테스트 작성 → 통과 확인 → 다음 기능
```

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

`application.properties`에 실제 DB 비밀번호나 JWT 시크릿 키가 들어갈 경우 `.gitignore`에 추가하거나 환경변수로 분리할 것. 현재는 H2 인메모리 DB 사용 중이라 문제없음.
