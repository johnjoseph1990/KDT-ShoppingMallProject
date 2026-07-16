import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { Button, TextInput } from '@vapor-ui/core'
import { login as loginApi } from '../api/auth'
import { useAuth } from '../context/AuthContext'

export default function LoginPage() {
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const { login } = useAuth()
  const navigate = useNavigate()

  // Vapor TextInput은 값 자체를 넘겨주므로, 필드명을 지정해 form 객체의 해당 키만 갱신
  const handleChange = (field) => (value) => setForm({ ...form, [field]: value })

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
        <TextInput
          type="email"
          placeholder="이메일"
          value={form.email}
          onValueChange={handleChange('email')}
          required
        />
        <TextInput
          type="password"
          placeholder="비밀번호"
          value={form.password}
          onValueChange={handleChange('password')}
          required
        />
        {error && <p style={styles.error}>{error}</p>}
        <Button type="submit" colorPalette="primary">
          로그인
        </Button>
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
  error: { color: 'red', margin: 0 },
}
