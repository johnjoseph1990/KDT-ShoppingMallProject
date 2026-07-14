import axiosInstance from './axiosInstance'

export const getCart = () => axiosInstance.get('/cart')
export const addToCart = (data) => axiosInstance.post('/cart', data)
export const updateCartItem = (cartItemId, data) => axiosInstance.put(`/cart/${cartItemId}`, data)
export const removeCartItem = (cartItemId) => axiosInstance.delete(`/cart/${cartItemId}`)
