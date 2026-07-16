import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { login as loginApi } from '../api/auth'
import { useAuth } from '../context/AuthContext'

export default function LoginPage() {
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const { login } = useAuth()
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    try {
      const res = await loginApi(form)
      login(res.data)
      navigate('/')
    } catch {
      setError('이메일 또는 비밀번호가 올바르지 않습니다.')
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
            style={{ margin: '0 0 12px', fontSize: 12, letterSpacing: '0.14em', color: '#75775e' }}
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
          {error && <p style={{ margin: 0, fontSize: 13, color: '#e63946' }}>{error}</p>}
          <button
            type="submit"
            style={{
              cursor: 'pointer',
              border: '1px solid #333330',
              background: '#333330',
              color: '#fffef2',
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
            color: '#6d6c61',
            fontWeight: 300,
          }}
        >
          계정이 없으신가요?{' '}
          <Link
            to="/signup"
            style={{
              color: '#333330',
              borderBottom: '1px solid #333330',
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
