import { useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { confirmPayment } from '../api/orders'
import { parseOrderId } from '../utils/toss'

// 토스 결제창이 카드 승인까지 마치고 successUrl로 리다이렉트해오는 도착 지점.
// 쿼리파라미터(paymentKey, orderId, amount)를 그대로 백엔드 승인(confirm) API로 전달한다.
export default function PaymentSuccessPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const [status, setStatus] = useState('confirming') // confirming | error
  // useRef: 리렌더링돼도 값이 유지되는 변수. React StrictMode의 effect 이중 실행이나
  // 새로고침으로 인해 승인 요청이 중복 전송되는 것을 막는다.
  const requested = useRef(false)

  useEffect(() => {
    if (requested.current) return
    requested.current = true

    const paymentKey = searchParams.get('paymentKey')
    const tossOrderId = searchParams.get('orderId') // "ORDER-1" 형태
    const amount = Number(searchParams.get('amount'))
    const orderId = parseOrderId(tossOrderId)

    confirmPayment(orderId, { paymentKey, tossOrderId, amount })
      .then(() => navigate(`/orders/${orderId}`, { replace: true }))
      .catch(() => setStatus('error'))
  }, [])

  if (status === 'error') {
    return (
      <div style={{ textAlign: 'center', padding: 60, fontSize: 14, color: '#6d6c61' }}>
        결제 승인 처리 중 문제가 발생했습니다. 주문 목록에서 상태를 다시 확인해주세요.
      </div>
    )
  }
  return (
    <div style={{ textAlign: 'center', padding: 60, fontSize: 14, color: '#6d6c61' }}>
      결제를 확인하는 중입니다...
    </div>
  )
}
