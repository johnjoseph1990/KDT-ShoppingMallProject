import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getOrders } from '../api/orders'

// 주문 상태를 한글로 변환
const STATUS_LABEL = {
  ORDERED: '주문완료',
  PAID: '결제완료',
  SHIPPING: '배송중',
  DELIVERED: '배송완료',
  CANCELED: '취소됨',
}

export default function OrderListPage() {
  const [orders, setOrders] = useState([])

  useEffect(() => {
    getOrders().then((res) => setOrders(res.data))
  }, [])

  return (
    <div style={{ maxWidth: '600px', margin: '0 auto' }}>
      <h2>주문 내역</h2>
      {orders.length === 0 ? (
        <p>주문 내역이 없습니다.</p>
      ) : (
        orders.map((order) => (
          <Link to={`/orders/${order.id}`} key={order.id} style={styles.cardLink}>
            <div style={styles.card}>
              <div style={styles.row}>
                <strong>주문 #{order.id}</strong>
                <span style={statusStyle(order.status)}>
                  {STATUS_LABEL[order.status] ?? order.status}
                </span>
              </div>
              <p style={styles.price}>{order.totalPrice.toLocaleString()}원</p>
              <p style={styles.date}>{new Date(order.createdAt).toLocaleString()}</p>
            </div>
          </Link>
        ))
      )}
    </div>
  )
}

// 상태에 따라 색상 다르게 표시
function statusStyle(status) {
  const colors = {
    ORDERED: '#f4a261',
    PAID: '#2a9d8f',
    SHIPPING: '#457b9d',
    DELIVERED: '#1d3557',
    CANCELED: '#e63946',
  }
  return { color: colors[status] ?? '#333', fontWeight: 'bold' }
}

const styles = {
  cardLink: { textDecoration: 'none', color: 'inherit' },
  card: {
    border: '1px solid #ddd',
    borderRadius: '8px',
    padding: '1rem',
    marginBottom: '0.75rem',
    transition: 'box-shadow 0.2s',
  },
  row: { display: 'flex', justifyContent: 'space-between', alignItems: 'center' },
  price: { margin: '0.25rem 0', fontWeight: 'bold', fontSize: '1.05rem' },
  date: { margin: 0, color: '#999', fontSize: '0.85rem' },
}
