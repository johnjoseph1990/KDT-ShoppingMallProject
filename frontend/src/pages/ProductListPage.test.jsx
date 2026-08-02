// DEF-8 회귀 방지 테스트 — 모바일에서 "검색" 버튼 글자가 세로로 쪼개지지 않게 지킨다.
//
// 한계를 먼저 밝힌다: jsdom에는 레이아웃 엔진이 없어서 "실제로 2줄로 그려지는가"는
// 여기서 확인할 수 없다(그건 Playwright로 390px에서 직접 측정했다).
// 대신 그 현상을 만든 CSS 조건 자체를 계약으로 못박는다 —
//   flex-shrink: 0  → 폭이 모자라도 버튼이 찌그러지지 않는다
//   white-space: nowrap → 찌그러지더라도 글자를 줄바꿈하지 않는다
// 둘 중 하나라도 사라지면 390px에서 "검"/"색"으로 분리된다.
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import ProductListPage from './ProductListPage'

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
