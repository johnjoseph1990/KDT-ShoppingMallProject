// DEF-9 회귀 방지 테스트 — 관리자 페이징의 "다음" 버튼이 마지막 페이지를 인식하게 지킨다.
//
// 원래 버그: 비활성화 조건이 `orders.length === 0`이었다.
// 이건 "이미 빈 페이지에 도착했을 때"만 잠긴다는 뜻이라, 사용자는 끝을 한 칸 지나쳐
// 빈 표를 본 뒤에야 끝인 걸 알게 된다. 백엔드가 주는 totalPages를 쓰면 미리 알 수 있다.
//
// 이 테스트는 DEF-8 테스트와 달리 jsdom으로 충분하다 —
// 픽셀이 아니라 "버튼의 disabled 속성"이라는 DOM 상태를 보는 것이기 때문이다.
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import AdminPage from './AdminPage'
import { getProducts } from '../api/products'
import { getAdminOrders } from '../api/admin'

// Spring의 Page 응답을 흉내 내는 헬퍼.
// 실제 응답에는 content 말고도 totalPages·last·totalElements가 함께 온다 —
// 그걸 프론트가 안 쓰고 버린 게 이 결함의 뿌리였다.
const pageOf = (content, { totalPages, number = 0 }) => ({
  data: {
    content,
    totalPages,
    totalElements: content.length,
    number,
    last: number >= totalPages - 1,
    first: number === 0,
  },
})

const PRODUCT = { id: 1, name: '사과', price: 3000, stockQuantity: 10, averageRating: 4.5 }
const ORDER = { id: 1, totalPrice: 3000, createdAt: '2026-08-02T10:00:00', status: 'PAID' }

vi.mock('../api/products', () => ({
  getProducts: vi.fn(),
  createProduct: vi.fn(),
  updateProduct: vi.fn(),
  deleteProduct: vi.fn(),
}))
vi.mock('../api/admin', () => ({
  getAdminOrders: vi.fn(),
  updateOrderStatus: vi.fn(),
  uploadImage: vi.fn(),
}))

// 대기 중인 API 응답이 화면 상태에 반영될 때까지 기다린다.
//
// 왜 필요한가 (2026-08-02 CI에서 실제로 깨졌다):
// 처음엔 '총액'이 뜨는 것만 기다렸는데, '총액'은 **표 머리글**이라 데이터와 상관없이
// 패널이 마운트되는 순간 바로 뜬다. 즉 "화면이 그려졌다"는 신호일 뿐
// "응답이 도착해 totalPages가 채워졌다"는 신호가 아니다.
// 그래서 로컬에서는 우연히 통과했지만 CI에서는 응답 반영 전에 단언이 실행돼 실패했다.
//
// act(async () => {})는 대기 중인 프로미스 콜백을 흘려보내고
// 그로 인한 리렌더까지 적용한 뒤 돌아온다 — 데이터가 0건이라 화면에
// 아무 변화가 없는 경우에도 쓸 수 있는 유일한 완료 신호다.
const settle = async () => {
  await act(async () => {})
}

// 주문 탭은 기본 선택이 아니라 클릭해야 마운트된다 (Tabs.Panel의 keepMounted=false)
const openOrdersTab = async () => {
  fireEvent.click(screen.getByText('주문 관리'))
  await waitFor(() => expect(screen.getByText('총액')).toBeInTheDocument()) // 패널 마운트
  await settle() // 응답(totalPages) 반영
}

const nextBtn = () => screen.getByRole('button', { name: '다음' })
const prevBtn = () => screen.getByRole('button', { name: '이전' })

describe('AdminPage 페이징 — 마지막 페이지 인식 (DEF-9)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('주문 탭: 페이지가 1개뿐이면 "다음"이 비활성화된다', async () => {
    // 실제 재현 조건 그대로 — 주문 3건, totalPages: 1. 목록에 내용은 있으므로
    // 옛 조건(orders.length === 0)은 false가 되어 버튼이 열려 있었다.
    getProducts.mockResolvedValue(pageOf([PRODUCT], { totalPages: 1 }))
    getAdminOrders.mockResolvedValue(pageOf([ORDER, ORDER, ORDER], { totalPages: 1 }))

    render(<AdminPage />)
    await openOrdersTab()

    expect(nextBtn()).toBeDisabled()
  })

  it('주문 탭: 다음 페이지가 남아 있으면 "다음"이 활성화된다', async () => {
    getProducts.mockResolvedValue(pageOf([PRODUCT], { totalPages: 1 }))
    getAdminOrders.mockResolvedValue(pageOf([ORDER], { totalPages: 3 }))

    render(<AdminPage />)
    await openOrdersTab()

    expect(nextBtn()).toBeEnabled()
  })

  it('주문 탭: 마지막 페이지로 이동하면 "다음"이 잠긴다', async () => {
    getProducts.mockResolvedValue(pageOf([PRODUCT], { totalPages: 1 }))
    // 2페이지 구성 — 0페이지에서는 열려 있고, 1페이지로 가면 잠겨야 한다
    getAdminOrders.mockImplementation((page) =>
      Promise.resolve(pageOf([ORDER], { totalPages: 2, number: page })),
    )

    render(<AdminPage />)
    await openOrdersTab()
    expect(nextBtn()).toBeEnabled()

    fireEvent.click(nextBtn())

    await waitFor(() => expect(screen.getByText('페이지 2')).toBeInTheDocument())
    await settle() // 2페이지 응답까지 반영시킨 뒤 판정한다
    expect(nextBtn()).toBeDisabled()
  })

  it('상품 탭: 같은 규칙이 적용된다 (조건이 양쪽에 복사돼 있었음)', async () => {
    getProducts.mockResolvedValue(pageOf([PRODUCT], { totalPages: 1 }))

    render(<AdminPage />)
    await waitFor(() => expect(screen.getByText('사과')).toBeInTheDocument())

    expect(nextBtn()).toBeDisabled()
  })

  it('첫 페이지에서는 "이전"이 비활성화된다 (기존 동작 유지)', async () => {
    getProducts.mockResolvedValue(pageOf([PRODUCT], { totalPages: 5 }))

    render(<AdminPage />)
    await waitFor(() => expect(screen.getByText('사과')).toBeInTheDocument())

    expect(prevBtn()).toBeDisabled()
    expect(nextBtn()).toBeEnabled()
  })

  it('결과가 0건이면 "다음"이 열려 있지 않다 (totalPages가 0으로 오는 경우)', async () => {
    // Spring은 결과가 아예 없으면 totalPages를 0으로 준다.
    // 여기서 조건을 잘못 쓰면 빈 화면에서 버튼이 열린 채로 남는다.
    getProducts.mockResolvedValue(pageOf([], { totalPages: 0 }))

    render(<AdminPage />)
    // 결과가 0건이면 화면에 새로 나타나는 텍스트가 없어서 기다릴 대상이 없다.
    // waitFor(...toBeDisabled())로 쓰면 "응답 도착 전 초기 상태"에서 곧바로 통과해버려
    // 정작 고장난 코드도 잡지 못하는 헛통과가 된다 — 그래서 settle()로 반영을 먼저 보장한다.
    await settle()
    expect(nextBtn()).toBeDisabled()
  })
})
