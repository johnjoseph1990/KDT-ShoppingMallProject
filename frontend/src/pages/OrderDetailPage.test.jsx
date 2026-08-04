// OrderDetailPage 결제수단 선택 UI 단위 테스트.
//
// 이 화면의 가장 깨지기 쉬운 계약은 "화면에 보이는 라벨"과 "토스 SDK에 넘기는 값"이
// 다르다는 점이다. requestPayment()의 첫 인자는 토스가 정한 '카드'/'가상계좌' 문자열이라
// 라벨을 다듬다가 value까지 바꾸면 결제창 자체가 열리지 않는다. 그래서 value를 테스트로
// 못박아 둔다 — 화면 문구는 앞으로도 바뀔 수 있지만 value는 바뀌면 안 된다.
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import OrderDetailPage from './OrderDetailPage'
import { getOrder } from '../api/orders'

// 실제 HTTP 없이 렌더링만 시키기 위해 API 모듈을 가짜로 바꾼다.
vi.mock('../api/orders', () => ({ getOrder: vi.fn(), checkDeposit: vi.fn() }))
// AuthProvider를 세우는 대신 useAuth의 반환값만 흉내 낸다.
vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({ user: { name: '테스터', email: 'test@example.com' } }),
}))
// 토스 SDK는 실제로 불러오지 않는다(결제창을 열지 않는 테스트이므로).
vi.mock('@tosspayments/payment-sdk', () => ({ loadTossPayments: vi.fn() }))

// 결제 대기(ORDERED) 상태의 최소 주문 데이터.
// status/payment만 바꿔 다른 상태를 만든다. 상태 문자열은 utils/orderStatus.js의
// ORDER_STATUS_LABEL 키(= 백엔드 OrderStatus enum)와 같아야 한다.
const orderOf = (status = 'ORDERED', payment = null) => ({
  data: {
    id: 7,
    status,
    payment,
    createdAt: '2026-08-04T10:00:00',
    totalPrice: 42000,
    items: [{ productName: '충남 금산 당근', quantity: 2, orderPrice: 5000 }],
  },
})

// useParams가 id를 읽을 수 있도록 실제 라우트 경로 안에서 렌더한다.
const renderPage = () =>
  render(
    <MemoryRouter initialEntries={['/orders/7']}>
      <Routes>
        <Route path="/orders/:id" element={<OrderDetailPage />} />
      </Routes>
    </MemoryRouter>,
  )

describe('OrderDetailPage — 결제수단 선택', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getOrder.mockResolvedValue(orderOf())
  })

  it('두 수단의 결과 안내 문구를 함께 보여준다', async () => {
    renderPage()
    // 사용자가 알고 싶은 건 수단 이름이 아니라 "고르면 언제 발송되는가"다.
    expect(await screen.findByText('결제 즉시 발송 준비가 시작됩니다')).toBeInTheDocument()
    expect(screen.getByText('입금이 확인된 뒤 발송됩니다')).toBeInTheDocument()
  })

  it('라디오 value는 토스 SDK 계약값(카드/가상계좌)을 유지한다', async () => {
    renderPage()
    // 라벨은 '신용·체크카드'로 바뀌었지만 SDK에 넘어가는 값은 '카드'여야 한다.
    // 라디오에는 toHaveValue()를 쓸 수 없다(선택 상태와 혼동되므로 jest-dom이 막는다).
    // value 속성 자체를 확인한다.
    const card = await screen.findByRole('radio', { name: /신용·체크카드/ })
    expect(card).toHaveAttribute('value', '카드')
    expect(screen.getByRole('radio', { name: /무통장입금/ })).toHaveAttribute('value', '가상계좌')
  })

  it('기본 선택은 카드이고, 결제 버튼에 총액이 표시된다', async () => {
    renderPage()
    expect(await screen.findByRole('radio', { name: /신용·체크카드/ })).toBeChecked()
    expect(screen.getByRole('button', { name: '42,000원 결제하기' })).toBeInTheDocument()
  })

  it('가상계좌를 선택하면 버튼 문구가 "입금 계좌 발급받기"로 바뀐다', async () => {
    renderPage()
    // 가상계좌는 이 버튼으로 발급만 되므로, 같은 "결제하기" 문구를 쓰면 착각을 유발한다.
    await userEvent.click(await screen.findByRole('radio', { name: /무통장입금/ }))
    expect(screen.getByRole('button', { name: '입금 계좌 발급받기' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /42,000원 결제하기/ })).not.toBeInTheDocument()
  })

  it('결제가 끝난 주문에는 결제수단 선택이 보이지 않는다', async () => {
    getOrder.mockResolvedValue(orderOf('PAID'))
    renderPage()
    // '결제완료'라는 한글 라벨이 떠야 PAID가 실제 상태값임이 함께 증명된다.
    // (orderStatusLabel은 모르는 값이면 코드를 그대로 내보내므로, 오타면 이 단언이 깨진다)
    expect(await screen.findByText(/결제완료/)).toBeInTheDocument()
    expect(screen.queryByRole('radio', { name: /신용·체크카드/ })).not.toBeInTheDocument()
  })

  it('가상계좌 발급 후 입금대기 상태에서는 선택 UI 대신 계좌 안내를 보여준다', async () => {
    // 버튼 문구를 "입금 계좌 발급받기"로 바꾼 결과 사용자가 실제로 도착하는 화면이다.
    // 이 시점엔 수단을 다시 고를 여지가 없으므로 선택 UI는 사라져야 한다.
    getOrder.mockResolvedValue(
      orderOf('WAITING_FOR_DEPOSIT', {
        virtualAccountBankCode: 20,
        virtualAccountNumber: '1234567890',
        virtualAccountDueDate: '2026-08-06T23:59:59',
      }),
    )
    renderPage()
    expect(await screen.findByText('입금 계좌 안내')).toBeInTheDocument()
    expect(screen.getByText(/우리은행 1234567890/)).toBeInTheDocument()
    expect(screen.queryByRole('radio', { name: /무통장입금/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /발급받기/ })).not.toBeInTheDocument()
  })
})
