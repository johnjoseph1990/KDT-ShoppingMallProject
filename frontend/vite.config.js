import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  test: {
    // include: vitest(단위 테스트)는 src 아래만 본다. e2e/ 의 Playwright 스펙(*.spec.js)을
    // vitest가 잘못 수집해 실행하면 @playwright/test API를 못 찾아 실패하므로, 대상을 분리한다.
    include: ['src/**/*.{test,spec}.{js,jsx}'],
    // jsdom: Node.js 환경에서 브라우저 DOM API(document, window 등)를 에뮬레이션한다.
    // 이 설정이 없으면 render()가 "document is not defined" 오류를 낸다.
    environment: 'jsdom',
    // globals: true — describe/it/expect를 import 없이 전역으로 쓸 수 있다 (Jest 방식과 동일).
    globals: true,
    // setupFiles: 각 테스트 파일 실행 전에 공통으로 실행할 설정 파일.
    // @testing-library/jest-dom의 커스텀 매처(toBeInTheDocument 등)를 전역에 등록한다.
    setupFiles: './src/test/setup.js',
  },
})
