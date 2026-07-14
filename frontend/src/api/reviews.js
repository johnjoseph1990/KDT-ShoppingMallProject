import axiosInstance from './axiosInstance'

export const getReviews = (productId) => axiosInstance.get(`/products/${productId}/reviews`)
export const createReview = (productId, data) =>
  axiosInstance.post(`/products/${productId}/reviews`, data)
export const deleteReview = (productId, reviewId) =>
  axiosInstance.delete(`/products/${productId}/reviews/${reviewId}`)
