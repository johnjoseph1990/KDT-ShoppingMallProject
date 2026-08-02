// DEF-12 회귀 방지 테스트 — 상품 상세의 "추천 상품" 카드가 진짜 링크인지 지킨다.
//
// 이 테스트가 왜 ProductCard.test.jsx가 아니라 여기에 있나:
// DEF-7에서 상품 카드를 stretched link로 고치고 회귀 테스트 3개를 ProductCard.test.jsx에
// 걸어뒀는데, 추천 섹션은 ProductCard를 쓰지 않고 페이지 안에 복제해둔 <div onClick>이라
// 그 보호를 전혀 못 받았다(=DEF-12). 컴포넌트 테스트는 그 컴포넌트를 쓰는 곳만 지킨다.
// 그래서 "이 페이지가 실제로 링크를 렌더하는가"를 페이지 단위로 못박는다.
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import ProductDetailPage from './ProductDetailPage'
import { getProduct, getRecommendations, getKeywords } from '../api/products'
import { getReviews } from '../api/reviews'

const PRODUCT = {
  id: 1,
  name: '충남 금산 당근',
  description: '흙당근입니다',
  price: 9000,
  imageUrl: 'https://example.com/carrot.jpg',
  stockQuantity: 10,
  averageRating: 4.5,
  viewCount: 3,
  tags: ['뿌리채소'],
}

// 추천 상품 2건 — 상세 페이지의 "이런 상품은 어떠세요?" 섹션에 그려진다
const RECOMMENDATIONS = [
  { ...PRODUCT, id: 21, name: '제주 감자' },
  { ...PRODUCT, id: 22, name: '강원 무' },
]

vi.mock('../api/products', () => ({
  getProduct: vi.fn(),
  getRecommendations: vi.fn(),
  getKeywords: vi.fn(),
}))
vi.mock('../api/cart', () => ({ addToCart: vi.fn() }))
vi.mock('../api/reviews', () => ({
  getReviews: vi.fn(),
  createReview: vi.fn(),
  updateReview: vi.fn(),
  deleteReview: vi.fn(),
}))
vi.mock('../context/AuthContext', () => ({ useAuth: () => ({ user: null }) }))
vi.mock('../context/CartContext', () => ({
  useCart: () => ({ refreshCart: vi.fn(), showToast: vi.fn(), showMessage: vi.fn() }),
}))

// useParams로 id를 받는 페이지라 Route 안에서 렌더해야 한다
const renderPage = () =>
  render(
    <MemoryRouter initialEntries={['/products/1']}>
      <Routes>
        <Route path="/products/:id" element={<ProductDetailPage />} />
      </Routes>
    </MemoryRouter>,
  )

describe('ProductDetailPage 추천 상품 — 가짜 링크 회귀 방지 (DEF-12)', () => {
  beforeEach(async () => {
    vi.clearAllMocks()
    getProduct.mockResolvedValue({ data: PRODUCT })
    getRecommendations.mockResolvedValue({ data: RECOMMENDATIONS })
    getKeywords.mockResolvedValue({ data: [] })
    getReviews.mockResolvedValue({ data: { content: [], totalElements: 0 } })
    renderPage()
    // 추천 섹션은 응답 이후에 렌더되므로 등장을 기다린다
    await waitFor(() => expect(screen.getByText('이런 상품은 어떠세요?')).toBeInTheDocument())
  })

  it('추천 상품이 role=link로 노출되고 href가 상세 경로를 가리킨다', async () => {
    // div onClick이었을 때는 role=link가 아예 없어 이 검사에서 바로 걸린다
    const link = await screen.findByRole('link', { name: '제주 감자' })
    expect(link).toHaveAttribute('href', '/products/21')
  })

  it('추천 상품 개수만큼 링크가 생긴다 (2건 → 2개)', async () => {
    await screen.findByRole('link', { name: '제주 감자' })
    expect(screen.getByRole('link', { name: '강원 무' })).toHaveAttribute('href', '/products/22')
  })

  it('추천 카드는 ProductCard를 재사용한다 (복제본이 아니다)', async () => {
    await screen.findByRole('link', { name: '제주 감자' })
    // stretched link가 성립하려면 링크가 .product-card-link이고
    // 그 조상이 .product-card(position:relative)여야 한다 — index.css의 계약이다.
    const link = screen.getByRole('link', { name: '제주 감자' })
    expect(link).toHaveClass('product-card-link')
    expect(link.closest('.product-card')).not.toBeNull()
  })

  it('그리드 열 수가 추천 개수를 따라간다 (빈 칸에 회색 배경이 비치는 것 방지)', async () => {
    // 4열로 고정하면 추천이 2개일 때 빈 칸 2개에 컨테이너 배경이 그대로 보인다.
    // 홈 베스트 섹션에서 이미 겪은 결함(DEF-4)이 이 복제본에도 남아 있었다.
    await screen.findByRole('link', { name: '제주 감자' })
    const grid = screen.getByRole('link', { name: '제주 감자' }).closest('.mobile-2col')
    expect(grid).toHaveStyle({ gridTemplateColumns: 'repeat(2,1fr)' })
  })

  it('추천 섹션에 "유령 클릭 요소"(cursor:pointer인데 링크·버튼이 아닌 것)가 없다', async () => {
    await screen.findByRole('link', { name: '제주 감자' })
    const section = screen.getByText('이런 상품은 어떠세요?').closest('section')
    const ghosts = [...section.querySelectorAll('*')].filter(
      (el) =>
        el.style.cursor === 'pointer' &&
        !['A', 'BUTTON'].includes(el.tagName) &&
        !el.closest('a') &&
        !el.closest('button'),
    )
    expect(ghosts).toHaveLength(0)
  })
})
