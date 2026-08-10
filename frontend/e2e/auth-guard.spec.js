import { test, expect } from '@playwright/test'

// [스모크 6] 인가 거부 경로 — "들어가면 안 되는 사람이 못 들어가는가".
//
// 기존 스모크 5개는 전부 해피패스(성공 경로)만 검증했다. 그런데 프론트의 인가 로직인
// PrivateRoute(components/PrivateRoute.jsx:11-12)는 단위 테스트도 E2E도 없어서,
// 라우팅 설정에서 <PrivateRoute>를 실수로 빠뜨려도 아무 테스트가 실패하지 않았다.
// 이 스펙은 그 "차단이 실제로 걸리는가"를 브라우저에서 관통 검증한다.
//
// 단위 테스트(PrivateRoute.test.jsx)와 역할이 다르다: 단위 테스트는 컴포넌트에 직접
// user를 주입해 분기를 보고, 여기서는 App.jsx의 라우팅에 그 컴포넌트가 실제로 물려 있는지를 본다.
const TEST_USER = { email: 'test@shop.com', password: 'test1234' }

// 로그인 헬퍼 — exact: true는 전역 Footer 뉴스레터 입력창("이메일 주소")과의 부분매칭을 막는다.
async function login(page, { email, password }) {
  await page.goto('/login')
  await page.getByPlaceholder('이메일', { exact: true }).fill(email)
  await page.getByPlaceholder('비밀번호', { exact: true }).fill(password)
  await page.getByRole('button', { name: '로그인' }).click()
}

test('비로그인 상태로 주문 내역에 접근하면 로그인 화면으로 보낸다', async ({ page }) => {
  // 로그인 절차 없이 바로 보호 경로로 진입한다.
  await page.goto('/orders')

  await expect(page).toHaveURL(/\/login/)
  // URL만 보면 부족하다. OrderListPage는 목록 조회 실패를 .catch(() => {})로 삼키기 때문에
  // 가드가 없어도 "0건의 주문" 화면이 멀쩡히 그려질 수 있다. 그 화면이 아예 없었음을 못박는다.
  await expect(page.getByRole('heading', { name: '주문 내역' })).toHaveCount(0)
})

test('비로그인 상태로 관리자 화면에 접근하면 로그인 화면으로 보낸다', async ({ page }) => {
  await page.goto('/admin')

  await expect(page).toHaveURL(/\/login/)
  // 리다이렉트만 보면 부족하다. 관리자 화면이 잠깐이라도 그려지지 않았음을 함께 못박는다.
  await expect(page.getByRole('heading', { name: '관리자 페이지' })).toHaveCount(0)
})

test('일반 사용자가 관리자 화면에 접근하면 홈으로 돌려보낸다', async ({ page }) => {
  // 가장 위험한 시나리오 — 로그인은 되어 있어서 첫 번째 가드(!user)는 통과한다.
  // role !== 'ADMIN' 검사만이 유일한 방어선이고, 이게 뚫리면 일반 사용자가
  // 상품 등록·주문 상태 변경 화면을 그대로 보게 된다.
  await login(page, TEST_USER)
  await expect(page).toHaveURL('/') // 세션이 서기 전에 이동하면 판정이 흔들리므로 기다린다

  await page.goto('/admin')

  await expect(page).toHaveURL('/')
  await expect(page.getByRole('heading', { name: '관리자 페이지' })).toHaveCount(0)
})

test('비밀번호가 틀리면 로그인 실패 문구를 보여준다', async ({ page }) => {
  // LoginPage.jsx:24-25의 catch 블록 — 백엔드가 401 + {"message": ...}를 내려주고
  // 프론트가 그 문구를 화면에 띄우는 전체 사슬을 한 번에 검증한다.
  await login(page, { email: TEST_USER.email, password: '틀린비밀번호' })

  // 로그인에 실패했으므로 로그인 화면에 그대로 머물러야 한다.
  await expect(page).toHaveURL(/\/login/)
  await expect(page.getByText(/올바르지 않습니다|실패/)).toBeVisible()
})
