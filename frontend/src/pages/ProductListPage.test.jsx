// DEF-8 회귀 방지 테스트 — 모바일에서 "검색" 버튼 글자가 세로로 쪼개지지 않게 지킨다.
//
// 한계를 먼저 밝힌다: jsdom에는 레이아웃 엔진이 없어서 "실제로 2줄로 그려지는가"는
// 여기서 확인할 수 없다(그건 Playwright로 390px에서 직접 측정했다).
// 대신 그 현상을 만든 CSS 조건 자체를 계약으로 못박는다 —
//   flex-shrink: 0  → 폭이 모자라도 버튼이 찌그러지지 않는다
//   white-space: nowrap → 찌그러지더라도 글자를 줄바꿈하지 않는다
// 둘 중 하나라도 사라지면 390px에서 "검"/"색"으로 분리된다.
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import ProductListPage from './ProductListPage'
// vi.mock으로 가짜가 된 모듈을 그대로 import하면 그 가짜 함수(mock)를 손에 넣는다.
// 테스트마다 다른 응답을 돌려주게 하려면 이렇게 참조를 얻어 mockResolvedValue를 써야 한다.
import { getProducts } from '../api/products'

// vi.mock: 해당 모듈을 통째로 가짜로 바꾼다. 실제 HTTP 요청 없이 렌더링만 시키기 위함.
vi.mock('../api/products', () => ({
  getProducts: vi.fn(() =>
    Promise.resolve({
      data: { content: [], totalElements: 0, totalPages: 1, number: 0 },
    }),
  ),
}))
vi.mock('../api/cart', () => ({ addToCart: vi.fn() }))

// 컨텍스트 훅도 Provider를 세우는 대신 반환값만 흉내 낸다.
vi.mock('../context/AuthContext', () => ({ useAuth: () => ({ user: null }) }))
vi.mock('../context/CartContext', () => ({
  useCart: () => ({ refreshCart: vi.fn(), showToast: vi.fn(), showMessage: vi.fn() }),
}))

const renderPage = () =>
  render(
    <MemoryRouter>
      <ProductListPage />
    </MemoryRouter>,
  )

describe('ProductListPage — 검색 버튼 레이아웃 (DEF-8)', () => {
  beforeEach(() => {
    renderPage()
  })

  it('검색 버튼은 줄바꿈되지 않는다 (한글이 글자 단위로 쪼개지는 것 방지)', () => {
    const button = screen.getByRole('button', { name: '검색' })
    expect(button).toHaveStyle({ whiteSpace: 'nowrap' })
  })

  it('검색 버튼은 좁은 화면에서 찌그러지지 않는다', () => {
    const button = screen.getByRole('button', { name: '검색' })
    expect(button).toHaveStyle({ flexShrink: '0' })
  })

  it('검색 입력창은 minWidth 0이라 줄어드는 몫을 대신 떠맡는다', () => {
    // 버튼이 안 줄어드는 대신 누군가는 줄어야 한다. 그 역할이 입력창이다.
    const input = screen.getByRole('searchbox', { name: '상품 검색' })
    expect(input).toHaveStyle({ minWidth: '0px' })
  })

  it('검색 폼은 넘칠 때 줄을 바꾼다 (가로 스크롤 방지)', () => {
    const form = screen.getByRole('searchbox', { name: '상품 검색' }).closest('form')
    expect(form).toHaveStyle({ flexWrap: 'wrap' })
  })
})

// ─── 페이저 회귀 테스트 ────────────────────────────────────────────
// 이 화면의 "다음" 버튼 조건은 원래 `(page + 1) * 10 >= totalElements`였다.
// 결과는 맞았지만 페이지 크기 10이 화면 코드에 복사돼 있었다 — 진짜 출처는
// 백엔드 ProductController의 @PageableDefault(size = 10)다.
// 지금은 utils/pagination의 isLastPage가 totalPages만 보고 판정한다.
// 아래 테스트들은 "이 화면이 페이지 크기를 몰라도 옳게 동작한다"를 못 박는다.
describe('ProductListPage — 페이저 경계 판정', () => {
  // 상품 n개를 담은 가짜 Page 응답. Spring이 실제로 내려주는 모양과 같다.
  const pageOf = ({ count, totalElements, totalPages }) => ({
    data: {
      content: Array.from({ length: count }, (_, i) => ({
        id: i + 1,
        name: `상품${i + 1}`,
        price: 1000,
        stock: 5,
      })),
      totalElements,
      totalPages,
      number: 0,
    },
  })

  const nextButton = () => screen.getByRole('button', { name: /다음/ })

  it('마지막 페이지면 "다음"이 잠긴다 (totalPages 1)', async () => {
    getProducts.mockResolvedValue(pageOf({ count: 10, totalElements: 10, totalPages: 1 }))
    renderPage()
    // 응답이 도착해 totalPages가 반영될 때까지 기다린다.
    // 초기값 0일 때도 잠겨 있으므로, 기다리지 않으면 "이미 통과"로 착각할 수 있다.
    await waitFor(() => expect(screen.getByText('10개의 상품 · 매주 화·금 수확분 기준')))
    expect(nextButton()).toBeDisabled()
  })

  it('뒤에 페이지가 더 있으면 "다음"이 열린다 (totalPages 3)', async () => {
    getProducts.mockResolvedValue(pageOf({ count: 10, totalElements: 25, totalPages: 3 }))
    renderPage()
    await waitFor(() => expect(nextButton()).toBeEnabled())
  })

  it('결과가 0건이면 "다음"이 열려 있지 않다 (totalPages 0)', async () => {
    // Spring은 조회 결과가 아예 없으면 totalPages를 0으로 준다.
    getProducts.mockResolvedValue(pageOf({ count: 0, totalElements: 0, totalPages: 0 }))
    renderPage()
    await waitFor(() => expect(screen.getByText('0개의 상품 · 매주 화·금 수확분 기준')))
    expect(nextButton()).toBeDisabled()
  })

  it('서버 페이지 크기가 20으로 바뀌어도 마지막 페이지를 옳게 판정한다', async () => {
    // 이 케이스가 하드코딩 제거의 핵심 근거다.
    // 옛 조건이라면 (0 + 1) * 10 >= 20 → 10 >= 20 → false → "다음"이 열려서
    // 사용자가 빈 2페이지로 넘어갔다. totalPages를 쓰면 크기와 무관하게 잠긴다.
    getProducts.mockResolvedValue(pageOf({ count: 20, totalElements: 20, totalPages: 1 }))
    renderPage()
    await waitFor(() => expect(screen.getByText('20개의 상품 · 매주 화·금 수확분 기준')))
    expect(nextButton()).toBeDisabled()
  })
})
