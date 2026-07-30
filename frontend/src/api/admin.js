import axiosInstance from './axiosInstance'

// status가 null이면 파라미터를 보내지 않아 전체 조회가 된다
export const getAdminOrders = (page = 0, status = null) =>
  axiosInstance.get('/admin/orders', { params: { page, ...(status && { status }) } })
export const updateOrderStatus = (orderId, status) =>
  axiosInstance.patch(`/admin/orders/${orderId}/status`, { status })

// 이미지 파일을 Azure Blob Storage에 업로드하고 공개 URL을 반환한다.
// FormData로 감싸야 multipart/form-data 형식으로 전송된다.
export const uploadImage = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return axiosInstance.post('/admin/upload', formData)
}
