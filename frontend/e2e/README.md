# E2E 스모크 테스트 (Playwright)

핵심 사용자 경로 5개를 실제 브라우저로 관통 검증한다. 단위 테스트(vitest)가 못 잡던
통합·화면·인가 결함을 **배포 전에** 잡는 것이 목적이다.

| 파일             | 경로                      | 막는 회귀                         |
| ---------------- | ------------------------- | --------------------------------- |
| `home.spec.js`   | 홈 상품 노출              | 홈 베스트 섹션 붕괴 (DEF-2/DEF-4) |
| `shop.spec.js`   | 목록 → 상세 → 담기 버튼   | 구매 진입점 유실                  |
| `signup.spec.js` | 회원가입 → 로그인 이동    | 가입 흐름                         |
| `cart.spec.js`   | 로그인 → 담기 → 주문 요약 | 배송비 표시/합계 불일치           |
| `admin.spec.js`  | 관리자 로그인 → 주문 관리 | 관리자 인가·페이징 화면 (DEF-9)   |

## 실행 전 준비 (전체 스택이 떠 있어야 한다)

```bash
# 1) DB
docker compose up -d
# 2) 백엔드 (8080)
cd backend && ./gradlew bootRun
# 3) (최초 1회) Playwright 브라우저 설치
cd frontend && npx playwright install chromium
```

프론트 dev 서버(5173)는 `playwright.config.js`의 `webServer`가 자동으로 띄운다.

## 실행

```bash
cd frontend
npm run e2e        # 헤드리스 실행
npm run e2e:ui     # UI 모드(디버깅)
```

## 경계

- **결제(Toss)는 외부 연동**이라 자동화 대상이 아니다 — E2E 경계는 "주문 요약 확인"까지.
- 체크아웃의 주소 입력은 Daum 우편번호 팝업(외부)이라 자동 제출은 수동 검증으로 남긴다.
- 결함을 새로 고칠 때는 이 스모크가 아니라 **그 결함을 재현하는 테스트**를 별도로 추가한다
  (CLAUDE.md "결함 처리 절차" 참고).
