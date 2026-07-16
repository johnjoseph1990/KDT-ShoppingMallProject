import { Link, useNavigate } from 'react-router-dom'
import { Button } from '@vapor-ui/core'
import { useAuth } from '../context/AuthContext'
import { logout as logoutApi } from '../api/auth'

// 모든 페이지 상단에 고정되는 내비게이션 바
export default function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = async () => {
    await logoutApi()
    logout() // 클라이언트 상태 초기화
    navigate('/login')
  }

  return (
    <nav style={styles.nav}>
      <Link to="/" style={styles.brand}>
        🛒 ShoppingMall
      </Link>
      <div style={styles.links}>
        {user ? (
          <>
            <span style={styles.greeting}>{user.name}님</span>
            <Link to="/cart">장바구니</Link>
            <Link to="/orders">주문내역</Link>
            {user.role === 'ADMIN' && <Link to="/admin">관리자</Link>}
            {/* colorPalette="contrast": 어두운 네비바 배경 위에서도 잘 보이는 색 팔레트 */}
            <Button onClick={handleLogout} variant="outline" colorPalette="contrast" size="sm">
              로그아웃
            </Button>
          </>
        ) : (
          <>
            <Link to="/login">로그인</Link>
            <Link to="/signup">회원가입</Link>
          </>
        )}
      </div>
    </nav>
  )
}

const styles = {
  nav: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '0.75rem 1.5rem',
    background: '#1a1a2e',
    color: '#fff',
  },
  brand: { color: '#fff', textDecoration: 'none', fontWeight: 'bold', fontSize: '1.2rem' },
  links: { display: 'flex', gap: '1rem', alignItems: 'center' },
  greeting: { color: '#aaa', fontSize: '0.9rem' },
}
