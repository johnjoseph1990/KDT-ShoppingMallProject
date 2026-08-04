import { defineConfig, devices } from '@playwright/test'

// Playwright: 실제 브라우저(Chromium)를 자동 조종해 사용자처럼 클릭·이동하며 검증하는
// E2E(End-to-End) 테스트 러너다. vitest(단위, jsdom)와 달리 프론트→백엔드→DB까지 관통해,
// "배포 후에야 발견되던" 통합·화면 결함(DEF-4/7/9/12 등)을 배포 전에 잡는 것이 목적이다.
//
// 전제: 이 스모크는 백엔드(8080)+DB가 떠 있어야 통과한다. `docker compose up -d`로 DB를,
//       `./gradlew bootRun`으로 백엔드를 먼저 띄운다. 프론트 dev 서버(5173)는 아래 webServer가
//       자동 기동한다(vite dev가 /api를 8080으로 프록시하므로 세션 쿠키 인증이 그대로 동작).
// CI 여부는 GitHub Actions가 넣어주는 CI 환경변수로 판별한다.
const isCI = !!process.env.CI

export default defineConfig({
  testDir: './e2e',
  // 세션 로그인·장바구니 등 서버 상태를 건드리는 흐름이라 병렬 대신 순차 실행이 안전하다.
  fullyParallel: false,
  // CI에서는 네트워크·타이밍 흔들림을 흡수하려 1회 재시도(로컬은 0 — 실패를 바로 본다).
  retries: isCI ? 1 : 0,
  // CI에서는 실패 분석용 HTML 리포트를 함께 생성해 아티팩트로 올린다.
  reporter: isCI ? [['list'], ['html', { open: 'never' }]] : 'list',
  // 기대 단언 대기시간. CI 콜드 스타트(첫 API 호출이 느림) 대비로 기본 5초 → 10초.
  expect: { timeout: 10000 },
  use: {
    baseURL: 'http://localhost:5173',
    // 실패로 재시도할 때만 추적 파일을 남겨 원인 분석을 돕는다.
    trace: 'on-first-retry',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  // 프론트 dev 서버 자동 기동. 로컬은 이미 떠 있으면 재사용하고, CI는 항상 새로 띄운다.
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: !isCI,
    timeout: 120000,
  },
})
