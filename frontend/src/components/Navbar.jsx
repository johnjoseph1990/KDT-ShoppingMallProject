import { useState } from 'react'
// Link: 단순 이동용 (브랜드 로고에 사용)
// NavLink: 현재 URL과 to가 일치하면 활성 상태를 알려주는 Link의 특수 버전 (메뉴 항목에 사용)
import { useNavigate, Link, NavLink } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'
import { logout as logoutApi } from '../api/auth'

// 이 파일은 화면 맨 위에 항상 보이는 메뉴(네비게이션 바)를 만드는 React 컴포넌트다.
// React에서는 이런 화면 조각을 "컴포넌트"라고 부르고, 함수처럼 작성한다.
export default function Navbar() {
  // useAuth()는 로그인한 사용자 정보와 로그아웃 함수를 가져오는 커스텀 훅이다.
  // 커스텀 훅은 여러 컴포넌트에서 공통으로 쓰는 상태/함수를 쉽게 꺼내 쓰게 해준다.
  const { user, logout } = useAuth()
  // useCart()는 장바구니 개수와 장바구니를 여는 함수를 가져온다.
  const { cartCount, openCart } = useCart()
  // useNavigate()는 페이지를 코드로 이동시키는 함수(navigate)를 얻을 때 사용한다.
  const navigate = useNavigate()
  // useState는 "화면에서 바뀌는 값"을 저장할 때 사용한다.
  // 여기서는 모바일 메뉴가 열려 있는지 닫혀 있는지 기억한다.
  const [menuOpen, setMenuOpen] = useState(false)
  // 메뉴를 닫는 동작을 여러 곳에서 재사용하려고 함수로 분리했다.
  const closeMenu = () => setMenuOpen(false)

  // 로그아웃 버튼을 눌렀을 때 실행되는 함수다.
  // 1) 서버에 로그아웃 요청
  // 2) 로컬 로그인 상태 제거
  // 3) 로그인 페이지로 이동
  const handleLogout = async () => {
    await logoutApi()
    logout()
    navigate('/login')
  }

  // "무엇을 둘러볼지"를 결정하는 콘텐츠 탐색 메뉴(primary nav)다.
  // 로그인 여부와 상관없이 항상 같은 항목을 보여준다.
  const primaryLinks = [
    { to: '/shop', label: '쇼핑' },
    { to: '/story', label: '농부 이야기' },
    { to: '/about', label: '브랜드' },
  ]

  // "내 계정/구매 흐름"을 담당하는 유틸리티 메뉴(utility nav)다.
  // primaryLinks와 성격이 다르므로 배열을 따로 두고, 화면에서도 구분선으로 분리해 보여준다.
  const accountLinks = user
    ? [
        // "마이페이지"라고 이름 붙였지만 실제로는 주문 목록(/orders) 페이지로 연결된다.
        // 주소록 등 별도의 마이페이지 기능은 아직 없고, 지금은 로그인한 사용자의
        // 진입점 역할만 한다 (추후 마이페이지가 별도로 생기면 그때 라우트를 나눈다).
        { to: '/orders', label: '마이페이지' },
        // 관리자만 관리자 페이지를 볼 수 있게 role을 확인한다.
        ...(user.role === 'ADMIN' ? [{ to: '/admin', label: '관리자' }] : []),
      ]
    : [
        // 로그인하지 않았으면 로그인/회원가입 메뉴를 보여준다.
        { to: '/login', label: '로그인' },
        { to: '/signup', label: '회원가입' },
      ]

  return (
    <>
      {/* 상단 공지 배너: 사이트 전체에서 공통으로 보이는 안내 문구다. */}
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

      {/* 헤더는 스크롤해도 화면 상단에 붙어 있도록 sticky로 설정한다. */}
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
        {/* Link는 react-router-dom이 제공하는 이동용 컴포넌트다.
            일반 <a>처럼 보이지만, 새로고침 없이 페이지 이동을 처리한다. */}
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

        {/* 데스크톱용 메뉴 영역이다. 화면이 작아지면 CSS 미디어쿼리로 숨겨진다.
            바깥쪽 gap(40)을 안쪽 그룹들의 gap(각 20)보다 크게 둬서, "탐색 메뉴 묶음"과
            "계정 메뉴 묶음"이 한 덩어리가 아니라 서로 다른 그룹으로 보이게 한다. */}
        <nav
          className="navbar-desktop-nav"
          style={{
            gap: 28,
            fontSize: 14,
            fontWeight: 300,
            letterSpacing: '0.03em',
            alignItems: 'center',
          }}
        >
          {/* 탐색 메뉴 묶음: 무엇을 둘러볼지 결정하는 콘텐츠 링크들 */}
          <div style={{ display: 'flex', gap: 34, alignItems: 'center' }}>
            {primaryLinks.map((l) => (
              <NavItem key={l.to} to={l.to}>
                {l.label}
              </NavItem>
            ))}
          </div>

          {/* 두 그룹을 나누는 세로 구분선 */}
          <span aria-hidden="true" style={{ width: 1, height: 18, background: '#dddaca' }} />

          {/* 계정/구매 메뉴 묶음: 로그인·회원가입·주문내역처럼 "내 계정" 성격의 링크들.
              색을 약간 옅게(#6d6c61) 둬서 탐색 메뉴보다 톤을 한 단계 낮춘다. */}
          <div
            style={{
              display: 'flex',
              gap: 20,
              alignItems: 'center',
              fontSize: 13,
              color: '#6d6c61',
            }}
          >
            {accountLinks.map((l) => (
              <NavItem key={l.to} to={l.to}>
                {l.label}
              </NavItem>
            ))}

            {/* 로그인한 경우에만 사용자 이름을 보여준다. */}
            {user && <span>{user.name}님</span>}
            {/* 로그인한 경우에만 로그아웃 버튼을 보여준다. */}
            {user && <OutlineBtn onClick={handleLogout}>로그아웃</OutlineBtn>}
          </div>

          {/* 검색 아이콘 자리만 미리 배치해둔다. 지금은 상품이 8개뿐이라 검색 기능
              자체는 우선순위가 낮지만, 나중에 상품이 늘어났을 때 네비게이션 레이아웃을
              다시 흔들지 않도록 자리를 먼저 잡아둔다. 클릭하면 아직 준비 중이라는
              안내만 보여준다. */}
          <SearchIconBtn />

          {/* 장바구니 버튼은 장바구니 사이드바를 여는 역할을 한다.
              카트 아이콘 + 담긴 개수 배지로 표시한다. */}
          <CartIconBtn count={cartCount} onClick={openCart} />
        </nav>

        {/* 모바일에서만 보이는 햄버거 버튼이다. 누르면 메뉴 열기/닫기가 바뀐다. */}
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

        {/* 모바일 드롭다운 메뉴다.
            menuOpen이 true일 때만 화면에 나타난다. */}
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
            {/* 탐색 메뉴 묶음 (데스크톱과 동일하게 먼저 보여준다).
                variant="bar"로 활성 행 왼쪽에 세로 막대를 표시한다. */}
            {primaryLinks.map((l) => (
              <NavItem key={l.to} to={l.to} onClick={closeMenu} variant="bar">
                <div style={{ padding: '12px 0' }}>{l.label}</div>
              </NavItem>
            ))}

            {/* 가로 구분선으로 탐색 메뉴와 계정 메뉴를 분리한다 */}
            <div style={{ borderTop: '1px solid #dddaca', margin: '4px 0' }} />

            {/* 계정/구매 메뉴 묶음. 톤을 옅게 둬서 위쪽 탐색 메뉴와 구분되게 한다. */}
            {accountLinks.map((l) => (
              <NavItem key={l.to} to={l.to} onClick={closeMenu} variant="bar">
                <div style={{ padding: '12px 0', fontSize: 13, color: '#6d6c61' }}>{l.label}</div>
              </NavItem>
            ))}

            {/* 검색은 아직 준비 중이지만, 자리는 데스크톱과 동일하게 모바일에도 둔다 */}
            <div style={{ padding: '12px 0' }}>
              <SearchIconBtn />
            </div>

            {/* 로그인 상태라면 사용자 이름을 아래쪽에 다시 보여준다. */}
            {user && (
              <div style={{ padding: '12px 0', fontSize: 13, color: '#6d6c61' }}>{user.name}님</div>
            )}
            {/* 모바일에서 로그아웃하면 메뉴를 먼저 닫고 로그아웃 처리한다. */}
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

// NavItem은 "현재 페이지 표시가 되는 메뉴 링크"를 만드는 작은 컴포넌트다.
// NavLink에 style을 함수로 넘기면 ({ isActive }) => 스타일 형태로 활성 여부를 받는다.
// variant로 활성 표시 모양을 고른다:
//   'underline' (데스크톱): 가로로 놓인 메뉴라 글자 아래 밑줄이 자연스럽다.
//   'bar' (모바일): 세로로 쌓인 행 메뉴라 행 왼쪽의 세로 막대가 더 잘 맞는다.
// onClick을 받을 수 있게 해두면, 모바일처럼 메뉴를 닫아야 할 때 재사용하기 좋다.
function NavItem({ to, onClick, variant = 'underline', children }) {
  return (
    <NavLink
      to={to}
      onClick={onClick}
      style={({ isActive }) =>
        variant === 'bar'
          ? {
              // 모바일: 활성 행 왼쪽에 세로 막대. 비활성은 투명 막대로 자리만 유지해 글자가 안 밀림.
              display: 'block',
              borderLeft: isActive ? '2px solid #333330' : '2px solid transparent',
              paddingLeft: 12,
            }
          : {
              // 데스크톱: 활성 항목 글자 아래 밑줄.
              borderBottom: isActive ? '1px solid #333330' : '1px solid transparent',
              paddingBottom: 3,
            }
      }
      onMouseEnter={(e) => (e.currentTarget.style.color = '#75775e')}
      onMouseLeave={(e) => (e.currentTarget.style.color = '')}
    >
      {children}
    </NavLink>
  )
}

// SearchIconBtn은 검색 기능이 아직 없는 상태에서 자리만 미리 잡아두는 버튼이다.
// 상품이 몇 개 안 될 때는 검색의 가치가 낮지만, 나중에 상품이 늘어나 실제로
// 검색을 붙일 때 네비게이션 레이아웃을 다시 건드리지 않아도 되게 하기 위함이다.
function SearchIconBtn() {
  return (
    <button
      onClick={() => alert('검색 기능은 준비 중입니다')}
      aria-label="검색"
      style={{
        cursor: 'pointer',
        background: 'transparent',
        border: 'none',
        padding: '7px 4px',
        // display:flex + 정렬로, 안의 svg가 버튼 높이에 딱 맞게 세로 가운데 정렬된다
        display: 'flex',
        alignItems: 'center',
        color: '#333330',
      }}
      onMouseEnter={(e) => (e.currentTarget.style.color = '#75775e')}
      onMouseLeave={(e) => (e.currentTarget.style.color = '#333330')}
    >
      {/* 인라인 SVG 돋보기 아이콘. 이모지와 달리 stroke 색/두께를 CSS로 제어할 수 있어
          사이트의 얇은 획·뉴트럴 톤과 통일된다. stroke="currentColor"로 두면 위 button의
          color 값을 그대로 따라가므로, hover 시 색이 함께 바뀐다. */}
      <svg
        width="18"
        height="18"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinecap="round"
      >
        <circle cx="11" cy="11" r="7" />
        <line x1="16" y1="16" x2="21" y2="21" />
      </svg>
    </button>
  )
}

// OutlineBtn은 링크가 아니라 "동작을 실행하는 버튼"을 만들 때 쓰는 컴포넌트다.
// 로그아웃, 장바구니 열기처럼 페이지 이동보다 기능 실행이 중요할 때 사용한다.
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

// CartIconBtn은 장바구니를 여는 버튼을 "카트 아이콘 + 개수 배지" 형태로 보여준다.
// count(담긴 개수)가 1 이상일 때만 아이콘 오른쪽 위에 작은 숫자 배지를 띄운다.
function CartIconBtn({ count, onClick }) {
  return (
    <button
      onClick={onClick}
      // 아이콘 텍스트가 없으므로 스크린리더용 설명을 넣는다. 개수도 함께 읽어주면 더 친절하다.
      aria-label={`장바구니, 담긴 상품 ${count}개`}
      style={{
        // position: relative — 안쪽 배지(absolute)의 위치 기준점이 된다.
        position: 'relative',
        cursor: 'pointer',
        background: 'transparent',
        border: 'none',
        padding: '7px 4px',
        display: 'flex',
        alignItems: 'center',
        color: '#333330',
      }}
      onMouseEnter={(e) => (e.currentTarget.style.color = '#75775e')}
      onMouseLeave={(e) => (e.currentTarget.style.color = '#333330')}
    >
      {/* 카트 아이콘: 바퀴 두 개(circle) + 카트 몸통(path) */}
      <svg
        width="20"
        height="20"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        <circle cx="9" cy="20" r="1" />
        <circle cx="18" cy="20" r="1" />
        <path d="M2 3h3l2.4 12.4a1 1 0 0 0 1 .8h9.2a1 1 0 0 0 1-.8L21 7H6" />
      </svg>

      {/* 개수 배지: 담긴 게 있을 때만 아이콘 오른쪽 위에 작은 원으로 표시 */}
      {count > 0 && (
        <span
          style={{
            position: 'absolute',
            top: -2,
            right: -4,
            minWidth: 16,
            height: 16,
            padding: '0 4px',
            borderRadius: 8,
            background: '#333330',
            color: '#fffef2',
            fontSize: 10,
            fontWeight: 500,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          {count}
        </span>
      )}
    </button>
  )
}
