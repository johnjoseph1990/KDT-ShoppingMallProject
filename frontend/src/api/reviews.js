import axiosInstance from './axiosInstance'

// [P1-5] params로 page/size를 받아 백엔드의 페이징 API와 연동한다.
// 미전달 시 백엔드 기본값(page=0, size=20)을 사용한다.
export const getReviews = (productId, params) =>
  axiosInstance.get(`/products/${productId}/reviews`, { params })
export const createReview = (productId, data) =>
  axiosInstance.post(`/products/${productId}/reviews`, data)
export const deleteReview = (productId, reviewId) =>
  axiosInstance.delete(`/products/${productId}/reviews/${reviewId}`)
