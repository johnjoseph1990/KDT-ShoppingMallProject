import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'
import { logout as logoutApi } from '../api/auth'

/* 모든 페이지 최상단에 고정되는 헤더 네비게이션 */
export default function Navbar() {
  const { user, logout } = useAuth()
  const { cartCount, openCart } = useCart()
  const navigate = useNavigate()
  // 모바일 화면에서 햄버거 버튼을 눌렀을 때 드롭다운 메뉴가 열려있는지 여부
  const [menuOpen, setMenuOpen] = useState(false)
  const closeMenu = () => setMenuOpen(false)

  const handleLogout = async () => {
    await logoutApi()
    logout()
    navigate('/login')
  }

  // 데스크톱 네비게이션과 모바일 드롭다운이 같은 링크 목록을 공유하도록
  // 배열로 뽑아둔다 (로그인 상태에 따라 항목이 달라짐)
  const navLinks = [
    { to: '/shop', label: '쇼핑' },
    { to: '/story', label: '농부 이야기' },
    { to: '/about', label: '브랜드' },
    ...(user
      ? [
          { to: '/orders', label: '주문내역' },
          ...(user.role === 'ADMIN' ? [{ to: '/admin', label: '관리자' }] : []),
        ]
      : [
          { to: '/login', label: '로그인' },
          { to: '/signup', label: '회원가입' },
        ]),
  ]

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

        {/* 데스크톱 네비게이션 — 768px 이하에서는 index.css의 미디어쿼리로 숨겨짐 */}
        <nav
          className="navbar-desktop-nav"
          style={{
            gap: 34,
            fontSize: 14,
            fontWeight: 300,
            letterSpacing: '0.03em',
            alignItems: 'center',
          }}
        >
          {navLinks.map((l) => (
            <NavItem key={l.to} to={l.to}>
              {l.label}
            </NavItem>
          ))}

          {user && <span style={{ fontSize: 13, color: '#6d6c61' }}>{user.name}님</span>}
          {user && <OutlineBtn onClick={handleLogout}>로그아웃</OutlineBtn>}

          {/* 장바구니 버튼 */}
          <OutlineBtn onClick={openCart}>장바구니 ({cartCount})</OutlineBtn>
        </nav>

        {/* 햄버거 버튼 — 768px 이하에서만 보임 (index.css) */}
        <button
          className="navbar-hamburger-btn"
          onClick={() => setMenuOpen((open) => !open)}
          aria-label="메뉴 열기"
          style={{
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: 22,
            background: 'transparent',
            border: 'none',
            cursor: 'pointer',
          }}
        >
          {menuOpen ? '✕' : '☰'}
        </button>

        {/* 모바일 드롭다운 메뉴. header가 position:sticky라 absolute 자식의 기준점이 되므로
            top:100%(헤더 바로 아래)로 두면 스크롤해도 항상 헤더 바로 밑에 붙어있는다. */}
        {menuOpen && (
          <div
            style={{
              position: 'absolute',
              top: '100%',
              left: 0,
              right: 0,
              zIndex: 49,
              background: '#fffef2',
              borderBottom: '1px solid #dddaca',
              display: 'flex',
              flexDirection: 'column',
              padding: '12px clamp(20px,5vw,72px) 24px',
              fontSize: 15,
            }}
          >
            {navLinks.map((l) => (
              <NavItem key={l.to} to={l.to} onClick={closeMenu}>
                <div style={{ padding: '12px 0' }}>{l.label}</div>
              </NavItem>
            ))}

            {user && (
              <div style={{ padding: '12px 0', fontSize: 13, color: '#6d6c61' }}>{user.name}님</div>
            )}
            {user && (
              <OutlineBtn
                onClick={() => {
                  closeMenu()
                  handleLogout()
                }}
              >
                로그아웃
              </OutlineBtn>
            )}
          </div>
        )}
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
