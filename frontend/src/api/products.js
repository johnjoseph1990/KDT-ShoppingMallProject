import axiosInstance from './axiosInstance'

export const getProducts = (params) => axiosInstance.get('/products', { params })
export const getBestProducts = () => axiosInstance.get('/products/best')
export const getProduct = (id) => axiosInstance.get(`/products/${id}`)
export const getRecommendations = (id) => axiosInstance.get(`/products/${id}/recommendations`)
export const getKeywords = (id) => axiosInstance.get(`/products/${id}/keywords`)
export const createProduct = (data) => axiosInstance.post('/products', data)
export const updateProduct = (id, data) => axiosInstance.put(`/products/${id}`, data)
export const deleteProduct = (id) => axiosInstance.delete(`/products/${id}`)
