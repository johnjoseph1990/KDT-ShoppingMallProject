import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createOrder } from '../api/orders'
import { useCart } from '../context/CartContext'

const fmt = (n) => n.toLocaleString('ko-KR') + '원'
const FREE_SHIP = 40000

/* 배송 정보 입력 + 주문 내역 확인 후 결제하는 체크아웃 페이지 */
export default function CartPage() {
  const navigate = useNavigate()
  const { cartItems, cartTotal, refreshCart } = useCart()
  const [form, setForm] = useState({ name: '', phone: '', address: '', note: '' })
  const [loading, setLoading] = useState(false)

  const ship = cartTotal === 0 || cartTotal >= FREE_SHIP ? 0 : 3500
  const grand = cartTotal + ship

  const setField = (key) => (e) => setForm({ ...form, [key]: e.target.value })

  const handleOrder = async (e) => {
    e.preventDefault()
    if (cartItems.length === 0) return
    setLoading(true)
    try {
      /* 장바구니의 모든 아이템으로 주문 생성 */
      const orderItems = cartItems.map((i) => ({ cartItemId: i.id }))
      const res = await createOrder({ items: orderItems })
      await refreshCart()
      navigate(`/orders/${res.data.id}`)
    } catch (err) {
      alert(err.response?.data?.message || '주문 실패')
    } finally {
      setLoading(false)
    }
  }

  const inputStyle = {
    border: '1px solid #dddaca',
    background: 'transparent',
    padding: '14px',
    fontSize: 14,
    outline: 'none',
    width: '100%',
    fontFamily: "'Noto Sans KR', sans-serif",
    fontWeight: 300,
  }

  return (
    /* form이 양쪽 패널을 감싸 왼쪽 입력값 검증 후 오른쪽 버튼에서 제출 가능 */
    <form
      onSubmit={handleOrder}
      style={{
        animation: 'fadeUp .4s ease both',
        flex: 1,
        display: 'grid',
        gridTemplateColumns: '1.2fr 1fr',
        borderBottom: '1px solid #dddaca',
        alignItems: 'start',
      }}
    >
      {/* 왼쪽: 배송 정보 폼 */}
      <div
        style={{
          padding: 'clamp(32px,5vw,72px)',
          display: 'flex',
          flexDirection: 'column',
          gap: 32,
          borderRight: '1px solid #dddaca',
        }}
      >
        <h1
          style={{
            margin: 0,
            fontFamily: "'Noto Serif KR', serif",
            fontWeight: 300,
            fontSize: 30,
          }}
        >
          주문하기
        </h1>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 20, maxWidth: 480 }}>
          <FormLabel label="받는 분">
            <input
              placeholder="이름"
              required
              style={inputStyle}
              onChange={setField('name')}
              value={form.name}
            />
          </FormLabel>
          <FormLabel label="연락처">
            <input
              placeholder="010-0000-0000"
              required
              style={inputStyle}
              onChange={setField('phone')}
              value={form.phone}
            />
          </FormLabel>
          <FormLabel label="배송 주소">
            <input
              placeholder="주소"
              required
              style={inputStyle}
              onChange={setField('address')}
              value={form.address}
            />
          </FormLabel>
          <FormLabel label="배송 메모">
            <textarea
              placeholder="부재 시 문 앞에 놓아주세요"
              rows={3}
              style={{ ...inputStyle, resize: 'vertical' }}
              onChange={setField('note')}
              value={form.note}
            />
          </FormLabel>
        </div>
      </div>

      {/* 오른쪽: 주문 내역 + 결제 버튼 */}
      <div
        style={{
          padding: 'clamp(32px,5vw,72px)',
          background: '#f6f4e6',
          display: 'flex',
          flexDirection: 'column',
          gap: 24,
          position: 'sticky',
          top: 72,
        }}
      >
        <h2
          style={{
            margin: 0,
            fontFamily: "'Noto Serif KR', serif",
            fontWeight: 400,
            fontSize: 20,
          }}
        >
          주문 내역
        </h2>

        {cartItems.length === 0 ? (
          <p style={{ fontSize: 14, color: '#6d6c61', fontWeight: 300 }}>
            장바구니가 비어 있습니다.
          </p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column' }}>
            {cartItems.map((item) => (
              <div
                key={item.id}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  padding: '12px 0',
                  borderBottom: '1px solid #dddaca',
                  fontSize: 14,
                  gap: 12,
                }}
              >
                <span style={{ fontWeight: 300 }}>
                  {item.productName} × {item.quantity}
                </span>
                <span>{fmt(item.price * item.quantity)}</span>
              </div>
            ))}
          </div>
        )}

        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            fontSize: 14,
            color: '#6d6c61',
          }}
        >
          <span>배송비</span>
          <span>{ship === 0 ? '무료' : fmt(ship)}</span>
        </div>
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            fontSize: 16,
            fontFamily: "'Noto Serif KR', serif",
          }}
        >
          <span>합계</span>
          <span>{fmt(grand)}</span>
        </div>

        <button
          type="submit"
          disabled={cartItems.length === 0 || loading}
          style={{
            cursor: cartItems.length === 0 || loading ? 'default' : 'pointer',
            border: '1px solid #333330',
            background: '#333330',
            color: '#fffef2',
            padding: '16px 22px',
            fontSize: 14,
            letterSpacing: '0.04em',
            opacity: cartItems.length === 0 ? 0.5 : 1,
          }}
        >
          {fmt(grand)} 결제하기
        </button>
        <span
          onClick={() => navigate('/shop')}
          style={{
            cursor: 'pointer',
            fontSize: 13,
            color: '#6d6c61',
            textAlign: 'center',
          }}
        >
          계속 쇼핑하기
        </span>
      </div>
    </form>
  )
}

function FormLabel({ label, children }) {
  return (
    <label
      style={{ display: 'flex', flexDirection: 'column', gap: 8, fontSize: 13, color: '#6d6c61' }}
    >
      {label}
      {children}
    </label>
  )
}
