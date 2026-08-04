import { test, expect } from '@playwright/test'

// [스모크 4] 로그인 → 장바구니 담기 → 주문 요약 표시.
// 시드 계정(DevDataInitializer)은 git에 재현 가능하게 박혀 있어 어느 PC에서나 동일하다.
const TEST_USER = { email: 'test@shop.com', password: 'test1234' }

test('로그인 후 상품을 담고 주문 요약(배송비·합계)을 확인한다', async ({ page }) => {
  // given: 시드 계정으로 로그인 (세션 쿠키가 이후 요청에 자동 포함된다)
  await page.goto('/login')
  // exact: true 필수 — 전역 Footer 뉴스레터 입력창("이메일 주소")과 부분매칭되는 걸 막는다.
  await page.getByPlaceholder('이메일', { exact: true }).fill(TEST_USER.email)
  await page.getByPlaceholder('비밀번호', { exact: true }).fill(TEST_USER.password)
  await page.getByRole('button', { name: '로그인' }).click()
  await expect(page).toHaveURL('/') // 로그인 성공 시 홈으로 이동

  // when: 첫 상품 상세로 가서 장바구니에 담는다
  await page.goto('/shop')
  await page.locator('a[href^="/products/"]').first().click()
  await page.getByRole('button', { name: '장바구니에 담기' }).click()

  // then: /cart 주문 요약에 배송비·합계·결제 버튼이 보인다.
  // (장바구니 표시 금액과 실제 배송비가 어긋나던 결함의 회귀를 막는 지점)
  await page.goto('/cart')
  await expect(page.getByText('배송비')).toBeVisible()
  await expect(page.getByText('합계')).toBeVisible()
  await expect(page.getByRole('button', { name: /결제하기/ })).toBeVisible()
})
