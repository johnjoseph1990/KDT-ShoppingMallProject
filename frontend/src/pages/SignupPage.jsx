import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { Button, TextInput } from '@vapor-ui/core'
import { signup } from '../api/auth'

export default function SignupPage() {
  const [form, setForm] = useState({ name: '', email: '', password: '' })
  const [error, setError] = useState('')
  const navigate = useNavigate()

  const handleChange = (field) => (value) => setForm({ ...form, [field]: value })

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
        <TextInput
          placeholder="이름"
          value={form.name}
          onValueChange={handleChange('name')}
          required
        />
        <TextInput
          type="email"
          placeholder="이메일"
          value={form.email}
          onValueChange={handleChange('email')}
          required
        />
        <TextInput
          type="password"
          placeholder="비밀번호 (8자 이상)"
          value={form.password}
          onValueChange={handleChange('password')}
          required
          minLength={8}
        />
        {error && <p style={styles.error}>{error}</p>}
        <Button type="submit" colorPalette="primary">
          가입하기
        </Button>
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
  error: { color: 'red', margin: 0 },
}
