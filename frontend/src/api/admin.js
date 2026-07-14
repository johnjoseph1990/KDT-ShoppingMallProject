import axiosInstance from './axiosInstance'

export const getAdminOrders = (page = 0) => axiosInstance.get('/admin/orders', { params: { page } })
export const updateOrderStatus = (orderId, status) =>
  axiosInstance.patch(`/admin/orders/${orderId}/status`, { status })
