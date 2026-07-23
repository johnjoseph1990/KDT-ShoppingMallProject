// 백엔드 OrderService.buildTossOrderId()와 반드시 동일한 포맷("ORDER-{id}")을 유지해야 한다.
// 토스 결제창에 넘기는 orderId와, 승인 API 호출 시 백엔드가 검증하는 orderId가 같아야 하기 때문.
export const buildTossOrderId = (orderId) => `ORDER-${orderId}`

// "ORDER-1" → 1 (우리 DB의 실제 주문 PK로 되돌리기)
export const parseOrderId = (tossOrderId) => tossOrderId?.replace('ORDER-', '')
