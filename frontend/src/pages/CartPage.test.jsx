// CartPage(체크아웃) 주문 내역 편집 기능 단위 테스트.
//
// 이 화면의 계약 두 가지를 못박는다.
//  1) 항목별 삭제·수량 변경이 "결정하는 화면"에 있어야 한다 — 전에는 드로어에만 있어서
//     배송지를 입력하다 항목을 빼려면 헤더 카트 아이콘으로 되돌아가야 했다.
//  2) 수량 1에서는 "−"로 삭제되지 않는다(B안). CartContext.changeQty는 0을 삭제로
//     처리하므로, 그 규칙에 그대로 노출되면 실수로 항목이 사라진다.
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import CartPage from './CartPage'

// useCart의 반환값을 테스트마다 갈아끼울 수 있게 바깥 변수에 담아둔다.
const cartMock = {
  cartItems: [],
  cartTotal: 0,
  refreshCart: vi.fn(),
  removeItem: vi.fn(),
  changeQty: vi.fn(),
}
vi.mock('../context/CartContext', () => ({ useCart: () => cartMock }))
// 저장된 배송지 조회는 렌더에 필요 없으므로 빈 배열로 막는다.
vi.mock('../api/addresses', () => ({ getAddresses: vi.fn(() => Promise.resolve({ data: [] })) }))
vi.mock('../api/orders', () => ({ createOrder: vi.fn() }))

const items = [
  { id: 11, productName: '충남 금산 당근', price: 5000, quantity: 2 },
  { id: 12, productName: '논산 설향 딸기', price: 12000, quantity: 1 },
]

const renderPage = () =>
  render(
    <MemoryRouter>
      <CartPage />
    </MemoryRouter>,
  )

describe('CartPage — 주문 내역 항목 편집', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    cartMock.cartItems = items
    cartMock.cartTotal = 22000
  })

  it('항목마다 삭제 버튼이 상품명과 함께 노출된다', () => {
    renderPage()
    // aria-label에 상품명을 넣어야 여러 삭제 버튼을 서로 구분할 수 있다.
    expect(screen.getByRole('button', { name: '충남 금산 당근 삭제' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '논산 설향 딸기 삭제' })).toBeInTheDocument()
  })

  it('삭제 버튼은 해당 항목 id로 removeItem을 호출한다', async () => {
    renderPage()
    await userEvent.click(screen.getByRole('button', { name: '충남 금산 당근 삭제' }))
    expect(cartMock.removeItem).toHaveBeenCalledWith(11)
  })

  it('+ 버튼은 수량을 1 늘려 changeQty를 호출한다', async () => {
    renderPage()
    await userEvent.click(screen.getByRole('button', { name: '충남 금산 당근 수량 늘리기' }))
    expect(cartMock.changeQty).toHaveBeenCalledWith(11, 3)
  })

  it('− 버튼은 수량을 1 줄여 changeQty를 호출한다', async () => {
    renderPage()
    await userEvent.click(screen.getByRole('button', { name: '충남 금산 당근 수량 줄이기' }))
    expect(cartMock.changeQty).toHaveBeenCalledWith(11, 1)
  })

  it('수량이 1이면 − 버튼이 비활성화되어 실수 삭제를 막는다 (B안)', async () => {
    renderPage()
    // 딸기는 수량 1 — changeQty(id, 0)이 삭제로 처리되므로 여기서 막아야 한다.
    const minus = screen.getByRole('button', { name: '논산 설향 딸기 수량 줄이기' })
    expect(minus).toBeDisabled()
    await userEvent.click(minus)
    expect(cartMock.changeQty).not.toHaveBeenCalled()
    expect(cartMock.removeItem).not.toHaveBeenCalled()
  })

  it('편집 버튼은 type="button"이라 폼을 제출하지 않는다', () => {
    // 이 목록은 <form> 안에 있다. type을 빠뜨리면 기본값이 submit이라
    // 삭제를 눌렀을 때 주문이 생성돼버린다.
    renderPage()
    expect(screen.getByRole('button', { name: '충남 금산 당근 삭제' })).toHaveAttribute(
      'type',
      'button',
    )
    expect(screen.getByRole('button', { name: '충남 금산 당근 수량 늘리기' })).toHaveAttribute(
      'type',
      'button',
    )
  })

  it('장바구니가 비면 안내 문구를 보여주고 결제 버튼을 비활성화한다', () => {
    cartMock.cartItems = []
    cartMock.cartTotal = 0
    renderPage()
    expect(screen.getByText('장바구니가 비어 있습니다.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /결제하기/ })).toBeDisabled()
  })
})
