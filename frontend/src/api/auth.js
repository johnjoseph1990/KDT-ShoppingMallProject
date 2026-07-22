import axiosInstance from './axiosInstance'

export const signup = (data) => axiosInstance.post('/auth/signup', data)
export const login = (data) => axiosInstance.post('/auth/login', data)
export const logout = () => axiosInstance.post('/auth/logout')
export const getMe = () => axiosInstance.get('/auth/me')
// 이름·비밀번호 수정 — PUT /api/members/me
export const updateMe = (data) => axiosInstance.put('/members/me', data)
// 회원 탈퇴 — DELETE /api/members/me
export const deleteMe = () => axiosInstance.delete('/members/me')
