import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { signup } from '../api/auth'

export default function SignupPage() {
  const [form, setForm] = useState({ name: '', email: '', password: '' })
  const [error, setError] = useState('')
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    try {
      await signup(form)
      navigate('/login')
    } catch (err) {
      setError(err.response?.data?.message || '회원가입에 실패했습니다.')
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
            회원가입
          </h1>
        </div>

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          <input
            placeholder="이름"
            required
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            style={inputStyle}
          />
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
            placeholder="비밀번호 (8자 이상)"
            required
            minLength={8}
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
            가입하기
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
          이미 계정이 있으신가요?{' '}
          <Link
            to="/login"
            style={{
              color: 'var(--color-fg)',
              borderBottom: '1px solid var(--color-fg)',
              paddingBottom: 1,
            }}
          >
            로그인
          </Link>
        </p>
      </div>
    </main>
  )
}
