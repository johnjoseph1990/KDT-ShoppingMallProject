import axiosInstance from './axiosInstance'

// status가 null이면 파라미터를 보내지 않아 전체 조회가 된다
export const getAdminOrders = (page = 0, status = null) =>
  axiosInstance.get('/admin/orders', { params: { page, ...(status && { status }) } })
export const updateOrderStatus = (orderId, status) =>
  axiosInstance.patch(`/admin/orders/${orderId}/status`, { status })
