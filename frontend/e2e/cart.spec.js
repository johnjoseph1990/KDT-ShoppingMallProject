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

  // when: 재고가 있는 상품 상세로 가서 장바구니에 담는다.
  //
  // 전에는 .first()로 목록 첫 상품을 골랐는데, 그 상품이 품절이면 상세 페이지의
  // "장바구니에 담기"가 disabled라 클릭이 영원히 재시도되며 실패했다(실제로 발생).
  // 재고는 주문·테스트로 계속 변하는 런타임 상태이므로, 스펙이 "첫 상품이 재고를
  // 갖고 있다"는 가정에 기대면 안 된다. 품절 카드는 버튼이 disabled + aria-label이
  // '품절된 상품'이므로(ProductCard.jsx:164-165), 담을 수 있는 카드만 골라낸다.
  await page.goto('/shop')
  const inStockCard = page
    .locator('article')
    .filter({ has: page.getByRole('button', { name: '장바구니에 담기' }) })
    .first()
  await inStockCard.locator('a[href^="/products/"]').first().click()
  // 상세 페이지 진입을 명시적으로 기다린다. 안 기다리면 /shop 목록에 남아 있는 상태에서
  // "장바구니에 담기"가 카드 10개에 매칭돼 strict mode 위반이 난다.
  await expect(page).toHaveURL(/\/products\/\d+/)
  // .first(): 상세 하단 추천 카드에도 같은 이름의 버튼이 있어(비동기 로드) 메인 버튼만 특정한다.
  await page.getByRole('button', { name: '장바구니에 담기' }).first().click()

  // then: /cart 주문 요약에 배송비·합계·결제 버튼이 보인다.
  // (장바구니 표시 금액과 실제 배송비가 어긋나던 결함의 회귀를 막는 지점)
  await page.goto('/cart')
  await expect(page.getByText('배송비')).toBeVisible()
  await expect(page.getByText('합계')).toBeVisible()
  await expect(page.getByRole('button', { name: /결제하기/ })).toBeVisible()

  // then: 주문 내역에서 항목을 바로 빼거나 수량을 고칠 수 있다.
  // 전에는 편집이 장바구니 드로어에만 있어서, 배송지를 입력하던 사용자가 항목을
  // 빼려면 헤더 카트 아이콘으로 되돌아가야 했다.
  // .first(): 장바구니에 이전 실행에서 담긴 항목이 남아 있을 수 있어 버튼이 여러 개
  // 매칭된다 — strict mode 위반을 피해 첫 항목만 확인한다.
  await expect(page.getByRole('button', { name: /삭제$/ }).first()).toBeVisible()
  await expect(page.getByRole('button', { name: /수량 늘리기$/ }).first()).toBeVisible()
})
