import axios from 'axios'

// 세션 쿠키 기반 인증 — withCredentials로 쿠키를 매 요청에 자동 포함
const axiosInstance = axios.create({
  baseURL: '/api',
  withCredentials: true,
})

export default axiosInstance
