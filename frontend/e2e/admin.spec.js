import { test, expect } from '@playwright/test'

// [스모크 5] 관리자 로그인 → 주문 관리 화면 진입.
// /admin은 adminOnly 보호 라우트라, 인가가 살아있어야 화면이 뜬다.
const ADMIN = { email: 'admin@shop.com', password: 'admin1234' }

test('관리자가 로그인해 주문 관리 화면을 연다', async ({ page }) => {
  await page.goto('/login')
  // exact: true 필수 — 전역 Footer 뉴스레터 입력창("이메일 주소")과 부분매칭돼 2개가 잡히는 걸 막는다.
  await page.getByPlaceholder('이메일', { exact: true }).fill(ADMIN.email)
  await page.getByPlaceholder('비밀번호', { exact: true }).fill(ADMIN.password)
  await page.getByRole('button', { name: '로그인' }).click()
  // 로그인 완료(홈으로 리다이렉트)를 기다린 뒤 이동한다. 안 기다리면 세션이 서기 전에
  // /admin 에 진입해 PrivateRoute가 로그인 페이지로 되돌린다(관리자 헤딩을 못 찾는 원인).
  await expect(page).toHaveURL('/')

  await page.goto('/admin')
  await expect(page.getByRole('heading', { name: '관리자 페이지' })).toBeVisible()

  // 주문 관리 탭으로 전환하면 주문 테이블 헤더가 뜬다.
  // (관리자 페이징이 마지막 페이지를 인식 못 하던 DEF-9 화면의 기본 렌더 회귀 방지)
  await page.getByRole('button', { name: '주문 관리' }).click()
  await expect(page.getByText('주문일시')).toBeVisible()
})
