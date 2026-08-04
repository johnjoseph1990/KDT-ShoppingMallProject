// HarvestPage(제철 기획전) 단위 테스트.
// 홈 히어로 "이번 주 수확물 보기" CTA가 도착하는 프로모션 페이지의 계약을 못박는다.
//
// 주의: getWeekLabel()은 실제 new Date()를 쓰므로 "8월 첫째 주" 같은 동적 문구는
// 단언하지 않는다(실행 날짜에 따라 깨지는 플래키 테스트가 됨). 대신 날짜와 무관한
// 정적 요소 — 기획전 배지, 혜택 스트립 문구, 상품 렌더, 빈 상태 — 만 검증한다.
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import HarvestPage from './HarvestPage'
import { getProducts } from '../api/products'

// getProducts를 가짜로 바꿔 실제 HTTP 없이 렌더링만 시킨다.
vi.mock('../api/products', () => ({ getProducts: vi.fn() }))
// CartContext는 Provider를 세우는 대신 반환값(showMessage)만 흉내 낸다.
vi.mock('../context/CartContext', () => ({
  useCart: () => ({ showMessage: vi.fn() }),
}))

const renderPage = () =>
  render(
    <MemoryRouter>
      <HarvestPage />
    </MemoryRouter>,
  )

// Spring이 내려주는 Page 응답 모양. content만 채운다.
const pageOf = (products) => ({
  data: { content: products, totalElements: products.length, totalPages: 1, number: 0 },
})

describe('HarvestPage — 프로모션 배너/혜택', () => {
  beforeEach(() => {
    // 테스트 간 mock 호출 카운트가 누적돼 플래키해지지 않게 초기화한다
    // (형제 테스트 ProductDetailPage.test.jsx와 동일한 패턴).
    vi.clearAllMocks()
    getProducts.mockResolvedValue(pageOf([]))
  })

  it('기획전 배지를 노출한다', () => {
    renderPage()
    expect(screen.getByText('이번 주 제철 기획전')).toBeInTheDocument()
  })

  it('혜택 스트립 3가지를 모두 노출한다', () => {
    renderPage()
    expect(screen.getByText('수확 당일 배송')).toBeInTheDocument()
    expect(screen.getByText('소농 직거래')).toBeInTheDocument()
    expect(screen.getByText('이번 주 한정 수량')).toBeInTheDocument()
  })

  it('전체 상품 보기 CTA가 /shop으로 연결된다', () => {
    renderPage()
    expect(screen.getByRole('link', { name: /전체 상품 보기/ })).toHaveAttribute('href', '/shop')
  })
})

describe('HarvestPage — 상품 로딩 상태', () => {
  it('태그 상품이 있으면 상품 링크로 렌더된다', async () => {
    getProducts.mockResolvedValue(
      pageOf([
        { id: 1, name: '충남 금산 당근', price: 5000, stockQuantity: 10, averageRating: 4.5 },
        { id: 2, name: '논산 설향 딸기', price: 12000, stockQuantity: 5, averageRating: 4.8 },
      ]),
    )
    renderPage()
    // 응답 도착 후 상품명 링크가 상세 경로로 렌더되는지 확인한다.
    await waitFor(() =>
      expect(screen.getByRole('link', { name: '충남 금산 당근' })).toHaveAttribute(
        'href',
        '/products/1',
      ),
    )
    expect(screen.getByRole('link', { name: '논산 설향 딸기' })).toHaveAttribute(
      'href',
      '/products/2',
    )
  })

  it('상품이 4개 미만이면 열 수를 개수에 맞춰 빈 칸을 없앤다 (DEF-4 방어)', async () => {
    // 열을 4로 고정하면 상품이 2개일 때 빈 칸 2개에 컨테이너 배경이 회색 박스로 비친다.
    // 그리드 컨테이너의 gridTemplateColumns가 상품 수(2)에 맞춰지는지 계약으로 못박는다.
    getProducts.mockResolvedValue(
      pageOf([
        { id: 1, name: '충남 금산 당근', price: 5000, stockQuantity: 10, averageRating: 4.5 },
        { id: 2, name: '논산 설향 딸기', price: 12000, stockQuantity: 5, averageRating: 4.8 },
      ]),
    )
    renderPage()
    const card = await screen.findByRole('link', { name: '충남 금산 당근' })
    // ProductCard 루트(<article>)의 부모가 곧 그리드 컨테이너 div다.
    const grid = card.closest('article').parentElement
    expect(grid).toHaveStyle({ gridTemplateColumns: 'repeat(2, 1fr)' })
  })

  it('태그 상품이 없으면 빈 상태 안내를 보여준다', async () => {
    getProducts.mockResolvedValue(pageOf([]))
    renderPage()
    await waitFor(() =>
      expect(screen.getByText('이번 주 수확물을 준비 중입니다.')).toBeInTheDocument(),
    )
  })

  it('"이번주수확" 태그로만 상품을 요청한다', () => {
    getProducts.mockResolvedValue(pageOf([]))
    renderPage()
    // 기획전 페이지의 핵심 계약: 전체 상품이 아니라 태그 필터로 조회해야 한다.
    expect(getProducts).toHaveBeenCalledWith(expect.objectContaining({ tag: '이번주수확' }))
  })
})
