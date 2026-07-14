import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

// 로그인하지 않은 사용자는 로그인 페이지로 리다이렉트
export default function PrivateRoute({ children, adminOnly = false }) {
  const { user } = useAuth()

  if (!user) return <Navigate to="/login" replace />
  if (adminOnly && user.role !== 'ADMIN') return <Navigate to="/" replace />

  return children
}
