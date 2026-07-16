import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'
import { logout as logoutApi } from '../api/auth'

/* 모든 페이지 최상단에 고정되는 헤더 네비게이션 */
export default function Navbar() {
  const { user, logout } = useAuth()
  const { cartCount, openCart } = useCart()
  const navigate = useNavigate()

  const handleLogout = async () => {
    await logoutApi()
    logout()
    navigate('/login')
  }

  return (
    <>
      {/* 공지 배너 */}
      <div
        style={{
          background: '#333326',
          color: '#e5e3d3',
          textAlign: 'center',
          padding: '11px 20px',
          fontSize: 13,
          letterSpacing: '0.04em',
          fontWeight: 300,
        }}
      >
        대전·충남 농가에서 매주 화요일과 금요일에 수확한 것들이 도착합니다 — 4만 원 이상 무료 배송
      </div>

      {/* 스티키 헤더 */}
      <header
        style={{
          position: 'sticky',
          top: 0,
          zIndex: 50,
          background: '#fffef2',
          borderBottom: '1px solid #dddaca',
          padding: '0 clamp(20px,5vw,72px)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          height: 72,
        }}
      >
        {/* 브랜드 로고 */}
        <span
          onClick={() => navigate('/')}
          style={{
            cursor: 'pointer',
            fontFamily: "'Noto Serif KR', serif",
            fontSize: 20,
            letterSpacing: '0.02em',
            fontWeight: 500,
            userSelect: 'none',
          }}
        >
          MINS <em style={{ fontWeight: 300 }}>Farmers Market</em>
        </span>

        {/* 네비게이션 링크 */}
        <nav
          style={{
            display: 'flex',
            gap: 34,
            fontSize: 14,
            fontWeight: 300,
            letterSpacing: '0.03em',
            alignItems: 'center',
          }}
        >
          <NavLink onClick={() => navigate('/shop')}>쇼핑</NavLink>
          <NavLink onClick={() => navigate('/story')}>농부 이야기</NavLink>
          <NavLink onClick={() => navigate('/about')}>브랜드</NavLink>

          {user ? (
            <>
              <NavLink onClick={() => navigate('/orders')}>주문내역</NavLink>
              {user.role === 'ADMIN' && (
                <NavLink onClick={() => navigate('/admin')}>관리자</NavLink>
              )}
              <span style={{ fontSize: 13, color: '#6d6c61' }}>{user.name}님</span>
              <OutlineBtn onClick={handleLogout}>로그아웃</OutlineBtn>
            </>
          ) : (
            <>
              <NavLink onClick={() => navigate('/login')}>로그인</NavLink>
              <NavLink onClick={() => navigate('/signup')}>회원가입</NavLink>
            </>
          )}

          {/* 장바구니 버튼 */}
          <OutlineBtn onClick={openCart}>장바구니 ({cartCount})</OutlineBtn>
        </nav>
      </header>
    </>
  )
}

function NavLink({ onClick, children }) {
  return (
    <span
      onClick={onClick}
      style={{ cursor: 'pointer' }}
      onMouseEnter={(e) => (e.currentTarget.style.color = '#75775e')}
      onMouseLeave={(e) => (e.currentTarget.style.color = '')}
    >
      {children}
    </span>
  )
}

function OutlineBtn({ onClick, children }) {
  return (
    <span
      onClick={onClick}
      style={{
        cursor: 'pointer',
        border: '1px solid #333330',
        padding: '7px 16px',
        fontSize: 13,
        userSelect: 'none',
      }}
      onMouseEnter={(e) => {
        e.currentTarget.style.background = '#333330'
        e.currentTarget.style.color = '#fffef2'
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.background = ''
        e.currentTarget.style.color = ''
      }}
    >
      {children}
    </span>
  )
}
