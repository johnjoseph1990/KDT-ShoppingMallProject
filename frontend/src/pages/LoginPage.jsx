import { useState } from 'react'
import { useNavigate, useLocation, Link } from 'react-router-dom'
import { login as loginApi } from '../api/auth'
import { useAuth } from '../context/AuthContext'

export default function LoginPage() {
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  // 다른 페이지에서 로그인이 필요해 이동해 온 경우, location.state.from에
  // "돌아갈 위치"가 담겨 있다 (PrivateRoute, 장바구니 담기 등에서 전달).
  const from = location.state?.from

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    try {
      const res = await loginApi(form)
      login(res.data)
      // from이 있으면 원래 보던 페이지로, 없으면 홈으로 이동
      navigate(from || '/', { replace: true })
    } catch {
      setError('이메일 또는 비밀번호가 올바르지 않습니다.')
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
  }

  return (
    <main
      style={{
        animation: 'fadeUp .4s ease both',
        flex: 1,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 'clamp(48px,8vw,110px) 20px',
      }}
    >
      <div
        style={{
          width: '100%',
          maxWidth: 400,
          display: 'flex',
          flexDirection: 'column',
          gap: 32,
        }}
      >
        <div style={{ textAlign: 'center' }}>
          <p
            style={{
              margin: '0 0 12px',
              fontSize: 12,
              letterSpacing: '0.14em',
              color: 'var(--color-fg-accent)',
            }}
          >
            계정
          </p>
          <h1
            style={{
              margin: 0,
              fontFamily: "'Noto Serif KR', serif",
              fontWeight: 300,
              fontSize: 'clamp(28px,3vw,36px)',
              lineHeight: 1.4,
            }}
          >
            로그인
          </h1>
          {/* from이 있다는 건 클릭이 아니라 보호된 페이지 접근 시도로 인해
              자동으로 이 페이지에 오게 됐다는 뜻 — 이유를 알려준다 */}
          {from && (
            <p style={{ margin: '10px 0 0', fontSize: 13, color: 'var(--color-fg-accent)' }}>
              계속하려면 로그인이 필요합니다.
            </p>
          )}
        </div>

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          <input
            type="email"
            placeholder="이메일"
            required
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
            style={inputStyle}
          />
          <input
            type="password"
            placeholder="비밀번호"
            required
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
            style={inputStyle}
          />
          {error && (
            <p style={{ margin: 0, fontSize: 13, color: 'var(--color-danger)' }}>{error}</p>
          )}
          <button
            type="submit"
            style={{
              cursor: 'pointer',
              border: '1px solid var(--color-fg)',
              background: 'var(--color-fg)',
              color: 'var(--color-bg)',
              padding: '16px',
              fontSize: 14,
              letterSpacing: '0.04em',
              marginTop: 8,
            }}
          >
            로그인
          </button>
        </form>

        <p
          style={{
            margin: 0,
            textAlign: 'center',
            fontSize: 13,
            color: 'var(--color-fg-muted)',
            fontWeight: 300,
          }}
        >
          계정이 없으신가요?{' '}
          <Link
            to="/signup"
            style={{
              color: 'var(--color-fg)',
              borderBottom: '1px solid var(--color-fg)',
              paddingBottom: 1,
            }}
          >
            회원가입
          </Link>
        </p>
      </div>
    </main>
  )
}
