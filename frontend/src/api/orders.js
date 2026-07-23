import axiosInstance from './axiosInstance'

export const createOrder = (data) => axiosInstance.post('/orders', data)
export const getOrders = () => axiosInstance.get('/orders')
export const getOrder = (orderId) => axiosInstance.get(`/orders/${orderId}`)

// 토스 결제창(successUrl 리다이렉트)에서 받은 paymentKey/tossOrderId/amount를 백엔드로 전달해
// 결제 승인을 요청한다. 백엔드가 서버 금액과 대조 후 실제 토스 승인 API를 호출한다.
export const confirmPayment = (orderId, { paymentKey, tossOrderId, amount }) =>
  axiosInstance.post(`/orders/${orderId}/pay`, {
    paymentKey,
    orderId: tossOrderId,
    amount,
  })
