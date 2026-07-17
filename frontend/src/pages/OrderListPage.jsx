import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getOrders } from '../api/orders'

const fmt = (n) => n.toLocaleString('ko-KR') + '원'
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
    getOrders()
      .then((res) => setOrders(res.data))
      .catch(() => {})
  }, [])

  return (
    <main
      style={{
        animation: 'fadeUp .4s ease both',
        flex: 1,
        padding: 'clamp(32px,5vw,64px) clamp(20px,5vw,72px)',
      }}
    >
      <h1
        style={{
          margin: '0 0 8px',
          fontFamily: "'Noto Serif KR', serif",
          fontWeight: 300,
          fontSize: 32,
        }}
      >
        주문 내역
      </h1>
      <p style={{ margin: '0 0 40px', fontSize: 14, color: '#6d6c61', fontWeight: 300 }}>
        {orders.length}건의 주문
      </p>

      {orders.length === 0 ? (
        <div
          style={{ display: 'flex', flexDirection: 'column', gap: 16, alignItems: 'flex-start' }}
        >
          <p style={{ fontSize: 14, color: '#6d6c61', fontWeight: 300 }}>주문 내역이 없습니다.</p>
          <Link
            to="/shop"
            style={{
              fontSize: 13,
              borderBottom: '1px solid #333330',
              paddingBottom: 1,
            }}
          >
            쇼핑하러 가기
          </Link>
        </div>
      ) : (
        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            borderTop: '1px solid #dddaca',
            maxWidth: 640,
          }}
        >
          {orders.map((order) => (
            <Link
              key={order.id}
              to={`/orders/${order.id}`}
              style={{
                padding: '24px 0',
                borderBottom: '1px solid #dddaca',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'flex-start',
              }}
              onMouseEnter={(e) => (e.currentTarget.style.background = '#f6f4e6')}
              onMouseLeave={(e) => (e.currentTarget.style.background = '')}
            >
              <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                <p
                  style={{
                    margin: 0,
                    fontSize: 14,
                    fontFamily: "'Noto Serif KR', serif",
                  }}
                >
                  주문 #{order.id}
                </p>
                <p style={{ margin: 0, fontSize: 12, color: '#6d6c61', fontWeight: 300 }}>
                  {new Date(order.createdAt).toLocaleString()}
                </p>
              </div>
              <div
                style={{ display: 'flex', flexDirection: 'column', gap: 6, alignItems: 'flex-end' }}
              >
                <p style={{ margin: 0, fontSize: 14 }}>{fmt(order.totalPrice)}</p>
                <p style={{ margin: 0, fontSize: 12, color: '#75775e', fontWeight: 300 }}>
                  {STATUS_LABEL[order.status] ?? order.status}
                </p>
              </div>
            </Link>
          ))}
        </div>
      )}
    </main>
  )
}
