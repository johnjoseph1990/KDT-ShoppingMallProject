import axiosInstance from './axiosInstance'

export const signup = (data) => axiosInstance.post('/auth/signup', data)
export const login = (data) => axiosInstance.post('/auth/login', data)
export const logout = () => axiosInstance.post('/auth/logout')
export const getMe = () => axiosInstance.get('/auth/me')
// 이름·비밀번호 수정 — PUT /api/members/me
export const updateMe = (data) => axiosInstance.put('/members/me', data)
// 회원 탈퇴 — DELETE /api/members/me (비밀번호 확인 필수)
// axios DELETE는 body를 { data: ... }로 전달해야 한다
export const deleteMe = (password) => axiosInstance.delete('/members/me', { data: { password } })
