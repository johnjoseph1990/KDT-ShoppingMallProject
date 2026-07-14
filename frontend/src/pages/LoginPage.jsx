import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { login as loginApi } from '../api/auth'
import { useAuth } from '../context/AuthContext'

export default function LoginPage() {
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const { login } = useAuth()
  const navigate = useNavigate()

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value })

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    try {
      const res = await loginApi(form)
      login(res.data) // AuthContext에 사용자 정보 저장
      navigate('/')
    } catch {
      setError('이메일 또는 비밀번호가 올바르지 않습니다.')
    }
  }

  return (
    <div style={styles.container}>
      <h2>로그인</h2>
      <form onSubmit={handleSubmit} style={styles.form}>
        <input
          name="email"
          type="email"
          placeholder="이메일"
          value={form.email}
          onChange={handleChange}
          style={styles.input}
          required
        />
        <input
          name="password"
          type="password"
          placeholder="비밀번호"
          value={form.password}
          onChange={handleChange}
          style={styles.input}
          required
        />
        {error && <p style={styles.error}>{error}</p>}
        <button type="submit" style={styles.btn}>
          로그인
        </button>
      </form>
      <p>
        계정이 없으신가요? <Link to="/signup">회원가입</Link>
      </p>
    </div>
  )
}

const styles = {
  container: { maxWidth: '400px', margin: '4rem auto', textAlign: 'center' },
  form: { display: 'flex', flexDirection: 'column', gap: '0.75rem' },
  input: { padding: '0.6rem', fontSize: '1rem', borderRadius: '4px', border: '1px solid #ccc' },
  btn: {
    padding: '0.7rem',
    background: '#1a1a2e',
    color: '#fff',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '1rem',
  },
  error: { color: 'red', margin: 0 },
}
