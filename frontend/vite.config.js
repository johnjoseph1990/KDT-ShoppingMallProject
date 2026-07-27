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
