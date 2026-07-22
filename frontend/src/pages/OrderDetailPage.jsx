import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getOrder, payOrder } from '../api/orders'

const fmt = (n) => n.toLocaleString('ko-KR') + '원'
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
      const res = await getOrder(id)
      setOrder(res.data)
    } catch (err) {
      alert(err.response?.data?.message || '결제 실패')
    }
  }

  if (!order)
    return (
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: '60vh',
          fontSize: 14,
          color: '#6d6c61',
          fontWeight: 300,
        }}
      >
        불러오는 중...
      </div>
    )

  return (
    <main style={{ animation: 'fadeUp .4s ease both', flex: 1 }}>
      {/* ─── 주문 완료 메시지 ─── */}
      <section
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: 'clamp(48px,8vw,110px) 20px',
          borderBottom: '1px solid #dddaca',
        }}
      >
        <div
          style={{
            textAlign: 'center',
            display: 'flex',
            flexDirection: 'column',
            gap: 22,
            alignItems: 'center',
            maxWidth: 520,
          }}
        >
          <p style={{ margin: 0, fontSize: 13, letterSpacing: '0.14em', color: '#75775e' }}>
            주문 #{order.id} · {STATUS_LABEL[order.status] ?? order.status}
          </p>
          <h1
            style={{
              margin: 0,
              fontFamily: "'Noto Serif KR', serif",
              fontWeight: 300,
              fontSize: 'clamp(26px,2.8vw,36px)',
              lineHeight: 1.6,
            }}
          >
            고맙습니다.
            <br />
            밭에서 준비를 시작합니다.
          </h1>
          <p
            style={{
              margin: 0,
              fontSize: 14,
              lineHeight: 1.9,
              color: '#6d6c61',
              fontWeight: 300,
            }}
          >
            가장 가까운 수확일에 맞춰 담아 보내드립니다.
            <br />
            발송이 시작되면 문자로 알려드릴게요.
          </p>
          <span
            onClick={() => navigate('/')}
            style={{
              cursor: 'pointer',
              fontSize: 13,
              letterSpacing: '0.05em',
              borderBottom: '1px solid #333330',
              paddingBottom: 2,
            }}
          >
            처음으로 돌아가기
          </span>
        </div>
      </section>

      {/* ─── 주문 상세 정보 ─── */}
      <section
        style={{
          padding: 'clamp(40px,5vw,72px) clamp(20px,5vw,72px)',
          maxWidth: 640,
        }}
      >
        <h2
          style={{
            margin: '0 0 24px',
            fontFamily: "'Noto Serif KR', serif",
            fontWeight: 300,
            fontSize: 22,
          }}
        >
          주문 상세
        </h2>
        <p style={{ margin: '0 0 20px', fontSize: 13, color: '#6d6c61', fontWeight: 300 }}>
          주문일시: {new Date(order.createdAt).toLocaleString()}
        </p>

        {/* 배송지 정보 — order에 deliveryAddress가 있을 때만 표시 */}
        {order.deliveryAddress && (
          <div
            style={{
              background: '#f6f4e6',
              padding: '16px 20px',
              marginBottom: 24,
              display: 'flex',
              flexDirection: 'column',
              gap: 6,
              fontSize: 13,
              fontWeight: 300,
              lineHeight: 1.8,
            }}
          >
            <p style={{ margin: 0, fontWeight: 400 }}>배송지</p>
            <p style={{ margin: 0 }}>
              {order.deliveryName} · {order.deliveryPhone}
            </p>
            <p style={{ margin: 0 }}>
              ({order.deliveryZipCode}) {order.deliveryAddress} {order.deliveryAddressDetail}
            </p>
            {order.deliveryNote && (
              <p style={{ margin: 0, color: '#6d6c61' }}>메모: {order.deliveryNote}</p>
            )}
          </div>
        )}

        <div style={{ display: 'flex', flexDirection: 'column', borderTop: '1px solid #dddaca' }}>
          {order.items?.map((item, i) => (
            <div
              key={i}
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                padding: '14px 0',
                borderBottom: '1px solid #dddaca',
                fontSize: 14,
              }}
            >
              <span style={{ fontWeight: 300 }}>
                {item.productName} × {item.quantity}개
              </span>
              <span>{fmt((item.orderPrice ?? 0) * item.quantity)}</span>
            </div>
          ))}
        </div>

        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            padding: '20px 0',
            fontSize: 16,
            fontFamily: "'Noto Serif KR', serif",
          }}
        >
          <span>합계</span>
          <span>{fmt(order.totalPrice)}</span>
        </div>

        {/* 결제대기 상태에서만 결제 버튼 노출 */}
        {order.status === 'ORDERED' && (
          <button
            onClick={handlePay}
            style={{
              cursor: 'pointer',
              border: '1px solid #333330',
              background: '#333330',
              color: '#fffef2',
              padding: '16px 22px',
              fontSize: 14,
              letterSpacing: '0.04em',
              width: '100%',
              marginBottom: 24,
            }}
          >
            결제하기
          </button>
        )}

        <div style={{ display: 'flex', gap: 24 }}>
          <span
            onClick={() => navigate('/orders')}
            style={{
              cursor: 'pointer',
              fontSize: 13,
              color: '#6d6c61',
              borderBottom: '1px solid #dddaca',
              paddingBottom: 1,
            }}
          >
            주문 목록
          </span>
          <span
            onClick={() => navigate('/shop')}
            style={{
              cursor: 'pointer',
              fontSize: 13,
              color: '#6d6c61',
              borderBottom: '1px solid #dddaca',
              paddingBottom: 1,
            }}
          >
            계속 쇼핑하기
          </span>
        </div>
      </section>
    </main>
  )
}
