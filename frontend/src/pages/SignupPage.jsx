import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { signup } from '../api/auth'

export default function SignupPage() {
  const [form, setForm] = useState({ name: '', email: '', password: '' })
  const [error, setError] = useState('')
  const navigate = useNavigate()

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value })

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    try {
      await signup(form)
      alert('회원가입 완료! 로그인해주세요.')
      navigate('/login')
    } catch (err) {
      setError(err.response?.data?.message || '회원가입에 실패했습니다.')
    }
  }

  return (
    <div style={styles.container}>
      <h2>회원가입</h2>
      <form onSubmit={handleSubmit} style={styles.form}>
        <input
          name="name"
          placeholder="이름"
          value={form.name}
          onChange={handleChange}
          style={styles.input}
          required
        />
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
          placeholder="비밀번호 (6자 이상)"
          value={form.password}
          onChange={handleChange}
          style={styles.input}
          required
        />
        {error && <p style={styles.error}>{error}</p>}
        <button type="submit" style={styles.btn}>
          가입하기
        </button>
      </form>
      <p>
        이미 계정이 있으신가요? <Link to="/login">로그인</Link>
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
