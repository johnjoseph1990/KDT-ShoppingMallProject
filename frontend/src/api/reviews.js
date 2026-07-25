import axiosInstance from './axiosInstance'

// [P1-5] params로 page/size를 받아 백엔드의 페이징 API와 연동한다.
// 미전달 시 백엔드 기본값(page=0, size=20)을 사용한다.
export const getReviews = (productId, params) =>
  axiosInstance.get(`/products/${productId}/reviews`, { params })
export const createReview = (productId, data) =>
  axiosInstance.post(`/products/${productId}/reviews`, data)
// 리뷰 수정 — 백엔드 PUT /api/products/{productId}/reviews/{reviewId}와 연결된다.
// PUT은 "리소스 전체를 새 값으로 교체"하는 의미이므로 rating·content를 항상 함께 보낸다.
// data 형태: { rating: 1~5, content: '내용' }
export const updateReview = (productId, reviewId, data) =>
  axiosInstance.put(`/products/${productId}/reviews/${reviewId}`, data)
export const deleteReview = (productId, reviewId) =>
  axiosInstance.delete(`/products/${productId}/reviews/${reviewId}`)
