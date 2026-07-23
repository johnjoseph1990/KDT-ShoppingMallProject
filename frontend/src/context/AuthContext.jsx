import { createContext, useContext, useEffect, useState } from 'react'
import { getMe } from '../api/auth'
import { setUnauthorizedHandler } from '../api/axiosInstance'

// 로그인 상태를 앱 전체에서 공유하기 위한 컨텍스트
const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  // user: 로그인된 사용자 정보 (null이면 비로그인). 세션 쿠키로 인증 상태 유지.
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  // 페이지 새로고침 시 서버에 요청해 세션이 살아있는지 확인
  useEffect(() => {
    getMe()
      .then((res) => setUser(res.data))
      .catch(() => setUser(null))
      .finally(() => setLoading(false))
  }, [])

  // axios 인터셉터에 "401이 오면 로그인 상태를 비운다"는 콜백을 등록한다.
  // 서버 세션이 만료·유실됐는데 프론트만 로그인 상태로 남는 불일치를 자동으로 해소해준다.
  // user가 null이 되면 PrivateRoute가 알아서 /login으로 이동시킨다.
  useEffect(() => {
    setUnauthorizedHandler(() => setUser(null))
  }, [])

  const login = (userData) => setUser(userData)

  const logout = () => setUser(null)

  if (loading) return null

  return <AuthContext.Provider value={{ user, login, logout }}>{children}</AuthContext.Provider>
}

// useAuth 훅: 컴포넌트에서 로그인 상태를 쉽게 가져다 쓰기 위한 단축 함수
export const useAuth = () => useContext(AuthContext)
