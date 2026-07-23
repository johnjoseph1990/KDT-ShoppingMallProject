import axios from 'axios'

// 세션 쿠키 기반 인증 — withCredentials로 쿠키를 매 요청에 자동 포함
const axiosInstance = axios.create({
  baseURL: '/api',
  withCredentials: true,
})

// 401(세션 만료·유실) 발생 시 실행할 콜백. 이 모듈은 React 컴포넌트 밖이라
// AuthContext의 logout이나 라우터 navigate를 직접 못 쓴다. 그래서 AuthProvider가
// 마운트될 때 자신의 정리 함수를 여기에 "주입(등록)"해두고, 인터셉터가 그걸 호출한다.
let onUnauthorized = null

// AuthProvider가 호출해 401 처리 콜백을 등록한다.
export const setUnauthorizedHandler = (handler) => {
  onUnauthorized = handler
}

// 응답 인터셉터: 모든 응답이 이 함수를 거친다. 성공 응답은 그대로 흘려보내고,
// 에러 응답 중 401만 골라서 자동 로그아웃 콜백을 실행한다.
axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    // error.config.url은 baseURL(/api)을 뺀 상대경로. 로그인/세션확인/로그아웃 같은
    // 인증 흐름 자체는 비로그인 상태에서 401이 정상적으로 날 수 있으므로 제외한다.
    // (예: 페이지 로드 시 getMe → /auth/me, 로그인 실패 → /auth/login)
    const url = error.config?.url ?? ''
    const isAuthFlow = url.startsWith('/auth/')

    if (error.response?.status === 401 && !isAuthFlow && onUnauthorized) {
      onUnauthorized()
    }

    // 여기서 처리했더라도 원래 호출부(try/catch)도 에러를 받을 수 있게 계속 전파한다.
    return Promise.reject(error)
  },
)

export default axiosInstance
