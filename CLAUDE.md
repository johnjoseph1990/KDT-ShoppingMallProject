# KDT ShoppingMall Project — Claude Code 가이드

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

## 민감 정보 주의

`application.properties`에 실제 DB 비밀번호나 JWT 시크릿 키가 들어갈 경우 `.gitignore`에 추가하거나 환경변수로 분리할 것. 현재는 H2 인메모리 DB 사용 중이라 문제없음.