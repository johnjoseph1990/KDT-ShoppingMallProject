# 아키텍처 리뷰 (2026-07-14)

백엔드(`backend/`)와 프론트엔드(`frontend/`) 구조를 조사하고 발견한 이슈를 우선순위대로 정리한 문서. 각 항목은 후속 작업 시 체크박스로 진행 상태를 관리한다.

## 종합 우선순위

| 순위 | 작업 | 이유 | 상태 |
|---|---|---|---|
| 1 | `GlobalExceptionHandler`에 `AccessDeniedException` 핸들러 추가 | 서비스 레이어(`OrderService`, `CartService`)에서 던지는 `AccessDeniedException`이 Spring Security 기본 처리로 넘어가 다른 예외들과 응답 형식이 달랐음 | ✅ 완료 |
| 2 | `DevDataInitializer` 관리자 계정 하드코딩 분리 | `admin@shop.com` / `admin1234`가 평문으로 소스에 커밋되어 있음. 배포 전 환경변수/프로퍼티로 분리 필요 | ⬜ 미착수 |
| 3 | 프론트엔드 API 클라이언트 설계 시 세션 쿠키 반영 | 백엔드는 JWT가 아닌 세션 쿠키 기반 인증(`HttpSessionSecurityContextRepository`). `fetch`는 `credentials: 'include'`, `axios`는 `withCredentials: true` 필수. CORS 설정 시 `allowCredentials(true)` + 정확한 origin 필요 | ⬜ 미착수 (프론트 미구현 상태) |
| 4 (선택) | catch-all 500 핸들러 추가 | 예상 못한 예외 발생 시 스택트레이스 노출 위험 방지 | ⬜ 미착수 |

## 상세 내용

### 1. AccessDeniedException 처리 (완료)

- 위치: `backend/src/main/java/com/kdt/shoppingmall/exception/GlobalExceptionHandler.java`
- 변경 전: 미처리 → Spring Security 기본 처리 → `{"timestamp":..., "status":403, "error":"Forbidden", ...}` 형태
- 변경 후: 다른 예외와 동일하게 `{"message": "..."}` 형태로 통일

### 2. 관리자 계정 하드코딩

- 위치: `backend/src/main/java/com/kdt/shoppingmall/config/DevDataInitializer.java`
- 현재: `admin@shop.com` / `admin1234` 평문 하드코딩
- 제안: 환경변수(`ADMIN_EMAIL`, `ADMIN_PASSWORD`) 또는 `application-local.yml`(git 제외)로 분리. 지금은 H2 인메모리 + 개발 전용이라 당장 급하지는 않지만, 실제 배포 프로필을 만들 때 반드시 처리

### 3. 프론트엔드 인증 방식 정합성

- 백엔드: `AuthController`가 세션 쿠키 기반 인증 사용 (JWT 아님)
- 프론트엔드는 현재 Vite 템플릿 그대로 (라우팅/API 클라이언트/상태관리 전부 미착수)
- 프론트 개발 착수 시, 관습적인 "JWT + localStorage" 패턴이 아니라 세션 쿠키 방식에 맞춰 API 레이어를 설계해야 함 (놓치면 로그인 후 인증 상태 유지 실패로 이어지는 흔한 실수)

### 4. catch-all 500 핸들러 (선택)

- 위치: `backend/src/main/java/com/kdt/shoppingmall/exception/GlobalExceptionHandler.java`
- 현재 7개(이제 8개) 예외만 개별 처리, 그 외 미분류 예외는 Spring Boot 기본 에러 응답으로 처리됨
- 우선순위가 낮은 이유: 지금은 도메인 예외가 대부분 커버되어 있고, 무분별한 500 핸들러는 실제 버그를 숨길 위험도 있어 신중히 검토 후 추가할 것

## 참고 (구조상 특이사항 — 즉시 조치 불필요)

- `controller`/`service`/`repository`/`exception` 패키지가 도메인별로 분리되지 않고 flat 구조 (`domain`/`dto`만 도메인별 하위 패키지). 현재 규모(5개 도메인)에서는 실용적인 선택으로 판단되며, 도메인이 늘어날 때 재검토
- `CartService`/`ReviewService`가 `ProductRepository`를 직접 참조. 순환 의존은 아니며, 현재 규모에서 별도 파사드 레이어를 추가하는 것은 과도한 추상화일 수 있음
- Payment가 독립된 컨트롤러/서비스 없이 `OrderService` 내부에 포함되어 있음. 결제 로직이 커지면 `OrderService`가 비대해질 수 있으니 그 시점에 분리 고려
