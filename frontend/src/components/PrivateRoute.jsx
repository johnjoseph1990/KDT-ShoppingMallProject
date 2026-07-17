import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

// 로그인하지 않은 사용자는 로그인 페이지로 리다이렉트
export default function PrivateRoute({ children, adminOnly = false }) {
  const { user } = useAuth()
  const location = useLocation()

  // state={{ from: location }}: 로그인 후 원래 가려던 페이지(예: /orders)로 돌아올 수 있도록
  // 현재 위치를 로그인 페이지에 함께 전달한다.
  if (!user) return <Navigate to="/login" replace state={{ from: location }} />
  if (adminOnly && user.role !== 'ADMIN') return <Navigate to="/" replace />

  return children
}
