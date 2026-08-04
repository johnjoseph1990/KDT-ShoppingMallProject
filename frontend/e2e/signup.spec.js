import { test, expect } from '@playwright/test'

// [스모크 3] 회원가입 → 로그인 페이지 이동.
// 회원가입은 런타임 데이터(실제 가입 회원)를 만든다. 로컬 dev DB 대상이라 허용하되,
// 매 실행 유니크한 이메일을 써서 "이미 가입된 이메일" 실패를 피한다(멱등하게 반복 실행 가능).
test('회원가입에 성공하면 로그인 페이지로 이동한다', async ({ page }) => {
  const email = `e2e_${Date.now()}@test.com`
  await page.goto('/signup')
  await page.getByPlaceholder('이름').fill('E2E테스터')
  await page.getByPlaceholder('이메일').fill(email)
  await page.getByPlaceholder('비밀번호 (8자 이상)').fill('test1234')
  await page.getByRole('button', { name: '가입하기' }).click()
  // SignupPage는 성공 시 navigate('/login') 한다.
  await expect(page).toHaveURL(/\/login$/)
})
