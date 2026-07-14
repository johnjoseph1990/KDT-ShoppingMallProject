import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getOrder, payOrder } from '../api/orders'

const STATUS_LABEL = {
  ORDERED: '주문완료',
  PAID: '결제완료',
  SHIPPING: '배송중',
  DELIVERED: '배송완료',
  CANCELED: '취소됨',
}

export default function OrderDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [order, setOrder] = useState(null)

  useEffect(() => {
    getOrder(id).then((res) => setOrder(res.data))
  }, [id])

  const handlePay = async () => {
    try {
      await payOrder(id)
      // 결제 후 최신 주문 상태 다시 로드
      const res = await getOrder(id)
      setOrder(res.data)
      alert('결제가 완료되었습니다.')
    } catch (err) {
      alert(err.response?.data?.message || '결제 실패')
    }
  }

  if (!order) return <p>로딩 중...</p>

  return (
    <div style={{ maxWidth: '560px', margin: '0 auto' }}>
      <button onClick={() => navigate('/orders')} style={styles.backBtn}>
        ← 목록으로
      </button>
      <h2>주문 #{order.id}</h2>
      <p>
        상태:{' '}
        <strong style={{ color: '#2a9d8f' }}>{STATUS_LABEL[order.status] ?? order.status}</strong>
      </p>
      <p>주문일시: {new Date(order.createdAt).toLocaleString()}</p>

      {/* 주문 상품 목록 */}
      <h3>주문 상품</h3>
      {order.items?.map((item, i) => (
        <div key={i} style={styles.itemRow}>
          <span>{item.productName}</span>
          <span>
            {item.price?.toLocaleString()}원 × {item.quantity}개
          </span>
          <span style={{ fontWeight: 'bold' }}>
            {((item.price ?? 0) * item.quantity).toLocaleString()}원
          </span>
        </div>
      ))}

      <div style={styles.total}>합계: {order.totalPrice.toLocaleString()}원</div>

      {/* 결제대기 상태일 때만 결제 버튼 표시 */}
      {order.status === 'ORDERED' && (
        <button onClick={handlePay} style={styles.payBtn}>
          결제하기
        </button>
      )}
    </div>
  )
}

const styles = {
  backBtn: {
    background: 'none',
    border: 'none',
    cursor: 'pointer',
    color: '#457b9d',
    fontSize: '0.95rem',
    padding: 0,
    marginBottom: '1rem',
  },
  itemRow: {
    display: 'flex',
    justifyContent: 'space-between',
    padding: '0.6rem 0',
    borderBottom: '1px solid #eee',
  },
  total: { textAlign: 'right', padding: '1rem 0', fontWeight: 'bold', fontSize: '1.1rem' },
  payBtn: {
    width: '100%',
    padding: '0.8rem',
    background: '#2a9d8f',
    color: '#fff',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '1rem',
  },
}
