import { useNavigate, Link } from 'react-router-dom'
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
        {/* 브랜드 로고 — Link를 쓰면 실제 <a> 태그가 되어 우클릭 "새 탭에서 열기", 접근성,
            SEO가 모두 자연스럽게 동작한다 (span+onClick은 마우스 클릭만 가능) */}
        <Link
          to="/"
          style={{
            fontFamily: "'Noto Serif KR', serif",
            fontSize: 20,
            letterSpacing: '0.02em',
            fontWeight: 500,
            userSelect: 'none',
          }}
        >
          MINS <em style={{ fontWeight: 300 }}>Farmers Market</em>
        </Link>

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
          <NavItem to="/shop">쇼핑</NavItem>
          <NavItem to="/story">농부 이야기</NavItem>
          <NavItem to="/about">브랜드</NavItem>

          {user ? (
            <>
              <NavItem to="/orders">주문내역</NavItem>
              {user.role === 'ADMIN' && <NavItem to="/admin">관리자</NavItem>}
              <span style={{ fontSize: 13, color: '#6d6c61' }}>{user.name}님</span>
              <OutlineBtn onClick={handleLogout}>로그아웃</OutlineBtn>
            </>
          ) : (
            <>
              <NavItem to="/login">로그인</NavItem>
              <NavItem to="/signup">회원가입</NavItem>
            </>
          )}

          {/* 장바구니 버튼 */}
          <OutlineBtn onClick={openCart}>장바구니 ({cartCount})</OutlineBtn>
        </nav>
      </header>
    </>
  )
}

// 순수 이동만 하는 링크는 <Link>로 만든다 (react-router-dom이 export하는
// NavLink와 이름이 겹치지 않도록 NavItem으로 명명). onClick은 필수는 아니지만
// 받을 수 있게 열어둬서, 모바일 메뉴에서 "링크 클릭 시 메뉴 닫기" 같은 용도로 쓸 수 있다.
function NavItem({ to, onClick, children }) {
  return (
    <Link
      to={to}
      onClick={onClick}
      onMouseEnter={(e) => (e.currentTarget.style.color = '#75775e')}
      onMouseLeave={(e) => (e.currentTarget.style.color = '')}
    >
      {children}
    </Link>
  )
}

// 로그아웃/장바구니 열기는 단순 이동이 아니라 부수 효과가 있는 동작이므로
// <a> 대신 진짜 <button>을 쓴다. 브라우저 기본 버튼 스타일(테두리/배경 등)을
// 리셋해줘야 기존 디자인과 동일하게 보인다.
function OutlineBtn({ onClick, children }) {
  return (
    <button
      onClick={onClick}
      style={{
        cursor: 'pointer',
        border: '1px solid #333330',
        background: 'transparent',
        color: 'inherit',
        fontFamily: 'inherit',
        padding: '7px 16px',
        fontSize: 13,
        userSelect: 'none',
      }}
      onMouseEnter={(e) => {
        e.currentTarget.style.background = '#333330'
        e.currentTarget.style.color = '#fffef2'
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.background = 'transparent'
        e.currentTarget.style.color = 'inherit'
      }}
    >
      {children}
    </button>
  )
}
