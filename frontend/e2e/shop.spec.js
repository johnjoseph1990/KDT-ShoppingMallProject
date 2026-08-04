import { test, expect } from '@playwright/test'

// [스모크 2] 목록 → 상세 이동 경로가 살아있는가.
// 상세 페이지가 열리면 "장바구니에 담기" 버튼이 보여야 한다(구매 진입점 회귀 방지).
test('상품 목록에서 상세로 이동하면 장바구니 버튼이 보인다', async ({ page }) => {
  await page.goto('/shop')
  // 목록의 첫 상품 카드를 클릭해 상세로 진입한다.
  await page.locator('a[href^="/products/"]').first().click()
  await expect(page.getByRole('button', { name: '장바구니에 담기' })).toBeVisible()
})
