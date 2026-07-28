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

// 가상계좌 발급 후 실제 입금이 됐는지 서버가 토스에 직접 물어보게 하는 폴링 요청.
export const checkDeposit = (orderId) => axiosInstance.post(`/orders/${orderId}/check-deposit`)
