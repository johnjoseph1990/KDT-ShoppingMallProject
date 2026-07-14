import axiosInstance from './axiosInstance'

export const createOrder = (data) => axiosInstance.post('/orders', data)
export const getOrders = () => axiosInstance.get('/orders')
export const getOrder = (orderId) => axiosInstance.get(`/orders/${orderId}`)
export const payOrder = (orderId) => axiosInstance.post(`/orders/${orderId}/pay`)
