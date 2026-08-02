import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createOrder } from '../api/orders'
import { useCart } from '../context/CartContext'
import { getAddresses } from '../api/addresses'

const fmt = (n) => n.toLocaleString('ko-KR') + '원'
// 주문 생성 전 미리보기용 값. 실제 배송비는 백엔드 Order.applyShippingFee()가
// 최종 계산해 주문에 반영한다(FREE_SHIPPING_THRESHOLD/SHIPPING_FEE). 이 숫자를 바꾸면
// 반드시 backend/.../domain/order/Order.java도 함께 바꿔야 미리보기와 실제 청구가 어긋나지 않는다.
const FREE_SHIP = 40000

/* 배송 정보 입력 + 주문 내역 확인 후 결제하는 체크아웃 페이지 */
export default function CartPage() {
  const navigate = useNavigate()
  const { cartItems, cartTotal, refreshCart } = useCart()
  const [form, setForm] = useState({
    name: '',
    phone: '',
    zipCode: '', // 우편번호 — 주소 검색 API가 자동으로 채워준다
    address: '', // 도로명/지번 주소 — 주소 검색 API가 자동으로 채워준다
    addressDetail: '', // 상세 주소 (동·호수 등) — 직접 입력
    note: '',
  })
  const [loading, setLoading] = useState(false)
  const [showAddressPicker, setShowAddressPicker] = useState(false)

  const ship = cartTotal === 0 || cartTotal >= FREE_SHIP ? 0 : 3500
  const grand = cartTotal + ship

  const setField = (key) => (e) => setForm({ ...form, [key]: e.target.value })

  // Daum 우편번호 서비스 스크립트를 컴포넌트 마운트 시 동적으로 로드한다.
  // index.html 대신 여기서 로드하면 CartPage를 방문할 때만 스크립트를 내려받는다.
  useEffect(() => {
    if (window.daum?.Postcode) return // 이미 로드돼 있으면 건너뜀
    const script = document.createElement('script')
    script.src = '//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js'
    script.async = true
    document.head.appendChild(script)
    return () => {
      // 컴포넌트 언마운트 시 스크립트 제거 (중복 로드 방지)
      document.head.removeChild(script)
    }
  }, [])

  // Daum 우편번호 팝업을 열고, 선택 완료 시 zipCode와 address를 자동으로 채운다.
  const openAddressSearch = () => {
    if (!window.daum?.Postcode) {
      alert('주소 검색 서비스를 불러오는 중입니다. 잠시 후 다시 시도해주세요.')
      return
    }
    new window.daum.Postcode({
      oncomplete: (data) => {
        // 사용자가 도로명/지번 중 선택한 주소를 사용한다
        const selectedAddress = data.roadAddress || data.jibunAddress
        setForm((prev) => ({
          ...prev,
          zipCode: data.zonecode,
          address: selectedAddress,
          addressDetail: '', // 주소가 바뀌면 상세 주소를 초기화
        }))
      },
    }).open()
  }

  const handleOrder = async (e) => {
    e.preventDefault()
    if (cartItems.length === 0) return
    setLoading(true)
    try {
      /* 장바구니 아이템 ID 목록과 배송지 정보를 함께 전송한다 */
      const res = await createOrder({
        cartItemIds: cartItems.map((i) => i.id),
        deliveryName: form.name,
        deliveryPhone: form.phone,
        deliveryZipCode: form.zipCode,
        deliveryAddress: form.address,
        deliveryAddressDetail: form.addressDetail,
        deliveryNote: form.note,
      })
      await refreshCart()
      navigate(`/orders/${res.data.id}`)
    } catch (err) {
      alert(err.response?.data?.message || '주문 실패')
    } finally {
      setLoading(false)
    }
  }

  const inputStyle = {
    border: '1px solid var(--color-border)',
    background: 'transparent',
    padding: '14px',
    fontSize: 14,
    outline: 'none',
    width: '100%',
    fontFamily: "'Noto Sans KR', sans-serif",
    fontWeight: 300,
    boxSizing: 'border-box',
  }

  return (
    <>
      {/* form이 양쪽 패널을 감싸 왼쪽 입력값 검증 후 오른쪽 버튼에서 제출 가능 */}
      {/* mobile-1col: 배송정보 폼(왼쪽)+주문요약(오른쪽) 2단 → 모바일에서 1단으로 접힘 */}
      <form
        onSubmit={handleOrder}
        className="mobile-1col"
        style={{
          animation: 'fadeUp .4s ease both',
          flex: 1,
          display: 'grid',
          gridTemplateColumns: '1.2fr 1fr',
          borderBottom: '1px solid var(--color-border)',
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
            borderRight: '1px solid var(--color-border)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between' }}>
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
            {/* 저장된 배송지가 있을 경우 한 번의 클릭으로 폼을 채울 수 있게 한다 */}
            <button
              type="button"
              onClick={() => setShowAddressPicker(true)}
              style={{
                background: 'none',
                border: '1px solid var(--color-border)',
                padding: '7px 14px',
                fontSize: 12,
                color: 'var(--color-fg-muted)',
                cursor: 'pointer',
                fontFamily: "'Noto Sans KR', sans-serif",
                letterSpacing: '0.03em',
                whiteSpace: 'nowrap',
              }}
            >
              저장된 배송지
            </button>
          </div>
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
              {/* 1행: 우편번호 + 주소 찾기 버튼 */}
              <div style={{ display: 'flex', gap: 8 }}>
                <input
                  placeholder="우편번호"
                  readOnly
                  required
                  style={{
                    ...inputStyle,
                    width: 120,
                    flexShrink: 0,
                    cursor: 'default',
                    color: '#555',
                  }}
                  value={form.zipCode}
                />
                <button
                  type="button"
                  onClick={openAddressSearch}
                  style={{
                    flexShrink: 0,
                    border: '1px solid var(--color-fg)',
                    background: 'var(--color-fg)',
                    color: 'var(--color-bg)',
                    padding: '0 16px',
                    fontSize: 13,
                    cursor: 'pointer',
                    fontFamily: "'Noto Sans KR', sans-serif",
                    whiteSpace: 'nowrap',
                  }}
                >
                  주소 찾기
                </button>
              </div>
              {/* 2행: 도로명/지번 주소 (API가 채워줌, 읽기 전용) */}
              <input
                placeholder="주소를 검색하세요"
                readOnly
                required
                style={{ ...inputStyle, cursor: 'default', color: '#555' }}
                value={form.address}
              />
              {/* 3행: 상세 주소 (직접 입력) */}
              <input
                placeholder="상세 주소 (동·호수 등)"
                style={inputStyle}
                onChange={setField('addressDetail')}
                value={form.addressDetail}
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
            background: 'var(--color-bg-hover-light)',
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
            <p style={{ fontSize: 14, color: 'var(--color-fg-muted)', fontWeight: 300 }}>
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
                    borderBottom: '1px solid var(--color-border)',
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
              color: 'var(--color-fg-muted)',
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
              border: '1px solid var(--color-fg)',
              background: 'var(--color-fg)',
              color: 'var(--color-bg)',
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
              color: 'var(--color-fg-muted)',
              textAlign: 'center',
            }}
          >
            계속 쇼핑하기
          </span>
        </div>
      </form>

      {/* 저장된 배송지 선택 모달 */}
      {showAddressPicker && (
        <AddressPickerModal
          onClose={() => setShowAddressPicker(false)}
          onSelect={(addr) => {
            setForm({
              name: addr.recipientName,
              phone: addr.phone,
              zipCode: addr.zipCode,
              address: addr.address,
              addressDetail: addr.addressDetail ?? '',
              note: addr.note ?? '',
            })
            setShowAddressPicker(false)
          }}
        />
      )}
    </>
  )
}

// 저장된 배송지 목록을 모달로 보여주고 하나를 선택하면 onSelect를 호출한다.
function AddressPickerModal({ onClose, onSelect }) {
  const [addresses, setAddresses] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getAddresses()
      .then((res) => setAddresses(res.data))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  // 기본 배송지 우선 정렬
  const sorted = [...addresses].sort((a, b) => (b.isDefault ? 1 : 0) - (a.isDefault ? 1 : 0))

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(0,0,0,0.45)',
        zIndex: 300,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 20,
      }}
      onClick={onClose}
    >
      <div
        style={{
          background: 'var(--color-bg)',
          padding: 'clamp(24px,4vw,40px)',
          maxWidth: 480,
          width: '100%',
          maxHeight: '80vh',
          overflowY: 'auto',
          display: 'flex',
          flexDirection: 'column',
          gap: 0,
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: 20,
          }}
        >
          <h2
            style={{
              fontFamily: "'Noto Serif KR', serif",
              fontWeight: 300,
              fontSize: 20,
              margin: 0,
            }}
          >
            배송지 선택
          </h2>
          <button
            onClick={onClose}
            style={{
              background: 'none',
              border: 'none',
              fontSize: 18,
              cursor: 'pointer',
              color: 'var(--color-fg-muted)',
            }}
          >
            ×
          </button>
        </div>

        {loading ? (
          <p style={{ fontSize: 14, color: 'var(--color-fg-muted)', fontWeight: 300 }}>
            불러오는 중...
          </p>
        ) : sorted.length === 0 ? (
          <p style={{ fontSize: 14, color: 'var(--color-fg-muted)', fontWeight: 300 }}>
            저장된 배송지가 없습니다. 마이페이지에서 배송지를 추가하세요.
          </p>
        ) : (
          sorted.map((addr) => (
            <div
              key={addr.id}
              onClick={() => onSelect(addr)}
              style={{
                padding: '16px 0',
                borderBottom: '1px solid var(--color-border)',
                cursor: 'pointer',
                display: 'flex',
                flexDirection: 'column',
                gap: 5,
              }}
              onMouseEnter={(e) =>
                (e.currentTarget.style.background = 'var(--color-bg-hover-light)')
              }
              onMouseLeave={(e) => (e.currentTarget.style.background = '')}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <span style={{ fontSize: 14 }}>{addr.recipientName}</span>
                <span style={{ fontSize: 12, color: 'var(--color-fg-muted)', fontWeight: 300 }}>
                  {addr.phone}
                </span>
                {addr.isDefault && (
                  <span
                    style={{
                      fontSize: 10,
                      letterSpacing: '0.06em',
                      color: 'var(--color-success)',
                      background: 'var(--color-success-bg)',
                      padding: '2px 7px',
                    }}
                  >
                    기본
                  </span>
                )}
              </div>
              <span style={{ fontSize: 13, fontWeight: 300 }}>
                ({addr.zipCode}) {addr.address} {addr.addressDetail ?? ''}
              </span>
            </div>
          ))
        )}
      </div>
    </div>
  )
}

function FormLabel({ label, children }) {
  return (
    <label
      style={{
        display: 'flex',
        flexDirection: 'column',
        gap: 8,
        fontSize: 13,
        color: 'var(--color-fg-muted)',
      }}
    >
      {label}
      {children}
    </label>
  )
}
