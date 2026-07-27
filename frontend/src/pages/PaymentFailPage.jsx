import { useNavigate, useSearchParams } from 'react-router-dom'
import { parseOrderId } from '../utils/toss'

// 사용자가 결제창을 취소했거나 카드사가 거절한 경우 토스가 리다이렉트하는 도착 지점.
// 결제 자체가 성사되지 않아 paymentKey가 없거나 무효하므로 백엔드 승인 호출은 필요 없다.
// 주문은 여전히 ORDERED 상태로 남아있어 사용자가 "결제하기"를 다시 누를 수 있다.
export default function PaymentFailPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const message = searchParams.get('message') || '결제가 취소되었거나 실패했습니다.'
  const orderId = parseOrderId(searchParams.get('orderId'))

  return (
    <div style={{ textAlign: 'center', padding: 60 }}>
      <p style={{ fontSize: 14, color: 'var(--color-fg-muted)', marginBottom: 24 }}>{message}</p>
      <button
        onClick={() => navigate(orderId ? `/orders/${orderId}` : '/orders')}
        style={{
          cursor: 'pointer',
          border: '1px solid var(--color-fg)',
          background: 'var(--color-fg)',
          color: 'var(--color-bg)',
          padding: '12px 22px',
          fontSize: 14,
        }}
      >
        주문으로 돌아가기
      </button>
    </div>
  )
}
