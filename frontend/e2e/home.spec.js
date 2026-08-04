import { test, expect } from '@playwright/test'

// [스모크 1] 홈 화면이 뜨고 상품이 노출되는가.
// 과거 홈 베스트 섹션은 판매 1건만 생겨도 붕괴하거나(DEF-2/DEF-4) 빈 회색 박스가 떴다.
// "상품 링크가 최소 1개는 렌더된다"만 지켜도 그 회귀를 배포 전에 잡는다.
test('홈 화면이 뜨고 상품 링크가 노출된다', async ({ page }) => {
  await page.goto('/')
  // 상품 카드는 /products/:id 로 가는 진짜 <a>다 (DEF-7에서 가짜 onClick div를 교체함).
  await expect(page.locator('a[href^="/products/"]')).not.toHaveCount(0)
})
