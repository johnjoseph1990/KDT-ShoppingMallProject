import { useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'

const FREE_SHIP = 40000
const fmt = (n) => n.toLocaleString('ko-KR') + '원'

/* 화면 우측에 슬라이드되는 장바구니 드로어 */
export default function CartDrawer() {
  const { user } = useAuth()
  const { cartItems, cartOpen, closeCart, cartCount, cartTotal, changeQty, removeItem } = useCart()
  const navigate = useNavigate()
  const location = useLocation()

  if (!cartOpen) return null

  const ship = cartTotal === 0 || cartTotal >= FREE_SHIP ? 0 : 3500
  const shipMsg =
    cartTotal >= FREE_SHIP
      ? '무료 배송으로 보내드립니다.'
      : `${fmt(FREE_SHIP - cartTotal)} 더 담으면 무료 배송입니다.`
  const isEmpty = cartItems.length === 0

  const handleCheckout = () => {
    closeCart()
    navigate('/cart')
  }

  return (
    <>
      {/* 배경 오버레이 */}
      <div
        onClick={closeCart}
        style={{
          position: 'fixed',
          inset: 0,
          background: 'rgba(40,40,30,0.4)',
          zIndex: 90,
        }}
      />

      {/* 드로어 패널 */}
      <aside
        style={{
          position: 'fixed',
          top: 0,
          right: 0,
          bottom: 0,
          width: 'min(440px, 100vw)',
          background: 'var(--color-bg)',
          zIndex: 100,
          display: 'flex',
          flexDirection: 'column',
          borderLeft: '1px solid var(--color-border)',
          animation: 'fadeUp .25s ease both',
        }}
      >
        {/* 헤더 */}
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '22px 28px',
            borderBottom: '1px solid var(--color-border)',
          }}
        >
          <h2
            style={{
              margin: 0,
              fontFamily: "'Noto Serif KR', serif",
              fontWeight: 400,
              fontSize: 18,
            }}
          >
            장바구니 ({cartCount})
          </h2>
          <button
            onClick={closeCart}
            style={{
              cursor: 'pointer',
              border: 'none',
              background: 'transparent',
              fontSize: 22,
              color: 'var(--color-fg-muted)',
              lineHeight: 1,
            }}
          >
            ×
          </button>
        </div>

        {/* 아이템 목록 */}
        <div style={{ flex: 1, overflowY: 'auto', padding: '8px 28px' }}>
          {!user && (
            <p
              style={{
                fontSize: 14,
                color: 'var(--color-fg-muted)',
                fontWeight: 300,
                lineHeight: 1.9,
                padding: '24px 0',
              }}
            >
              로그인 후 장바구니를 이용할 수 있습니다.
            </p>
          )}
          {user && isEmpty && (
            <p
              style={{
                fontSize: 14,
                color: 'var(--color-fg-muted)',
                fontWeight: 300,
                lineHeight: 1.9,
                padding: '24px 0',
              }}
            >
              아직 비어 있습니다.
              <br />
              이번 주 수확물을 둘러보세요.
            </p>
          )}
          {user &&
            cartItems.map((item) => (
              <div
                key={item.id}
                style={{
                  display: 'flex',
                  gap: 16,
                  padding: '20px 0',
                  borderBottom: '1px solid var(--color-border)',
                }}
              >
                <div
                  style={{
                    width: 64,
                    height: 80,
                    background: 'var(--color-bg-hover)',
                    flexShrink: 0,
                    overflow: 'hidden',
                  }}
                >
                  {item.imageUrl && (
                    <img
                      src={item.imageUrl}
                      alt={item.productName}
                      style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                    />
                  )}
                </div>
                <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 8 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', gap: 10 }}>
                    <p
                      style={{
                        margin: 0,
                        fontSize: 14,
                        fontFamily: "'Noto Serif KR', serif",
                      }}
                    >
                      {item.productName}
                    </p>
                    <button
                      onClick={() => removeItem(item.id)}
                      style={{
                        cursor: 'pointer',
                        border: 'none',
                        background: 'transparent',
                        color: 'var(--color-fg-muted)',
                        fontSize: 13,
                        flexShrink: 0,
                      }}
                    >
                      삭제
                    </button>
                  </div>
                  <div
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                    }}
                  >
                    <div style={{ display: 'flex', border: '1px solid var(--color-border)' }}>
                      <button
                        onClick={() => changeQty(item.id, item.quantity - 1)}
                        style={{
                          cursor: 'pointer',
                          border: 'none',
                          background: 'transparent',
                          width: 30,
                          padding: '5px 0',
                          fontSize: 14,
                        }}
                      >
                        −
                      </button>
                      <span
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          width: 26,
                          fontSize: 13,
                        }}
                      >
                        {item.quantity}
                      </span>
                      <button
                        onClick={() => changeQty(item.id, item.quantity + 1)}
                        style={{
                          cursor: 'pointer',
                          border: 'none',
                          background: 'transparent',
                          width: 30,
                          padding: '5px 0',
                          fontSize: 14,
                        }}
                      >
                        +
                      </button>
                    </div>
                    <span style={{ fontSize: 14 }}>{fmt(item.price * item.quantity)}</span>
                  </div>
                </div>
              </div>
            ))}
        </div>

        {/* 하단 결제 영역 */}
        <div
          style={{
            padding: '22px 28px',
            borderTop: '1px solid var(--color-border)',
            display: 'flex',
            flexDirection: 'column',
            gap: 14,
          }}
        >
          {user ? (
            <>
              <p
                style={{
                  margin: 0,
                  fontSize: 13,
                  color: 'var(--color-fg-accent)',
                  fontWeight: 300,
                }}
              >
                {shipMsg}
              </p>
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  fontSize: 15,
                  fontFamily: "'Noto Serif KR', serif",
                }}
              >
                <span>소계</span>
                <span>{fmt(cartTotal)}</span>
              </div>
              <button
                onClick={handleCheckout}
                disabled={isEmpty}
                style={{
                  cursor: isEmpty ? 'default' : 'pointer',
                  border: '1px solid var(--color-fg)',
                  background: 'var(--color-fg)',
                  color: 'var(--color-bg)',
                  padding: '16px',
                  fontSize: 14,
                  letterSpacing: '0.04em',
                  opacity: isEmpty ? 0.5 : 1,
                }}
              >
                주문하기
              </button>
            </>
          ) : (
            <button
              onClick={() => {
                closeCart()
                navigate('/login', { state: { from: location } })
              }}
              style={{
                cursor: 'pointer',
                border: '1px solid var(--color-fg)',
                background: 'var(--color-fg)',
                color: 'var(--color-bg)',
                padding: '16px',
                fontSize: 14,
                letterSpacing: '0.04em',
              }}
            >
              로그인하기
            </button>
          )}
        </div>
      </aside>
    </>
  )
}
