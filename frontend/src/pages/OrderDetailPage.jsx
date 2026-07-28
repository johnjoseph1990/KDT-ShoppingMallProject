import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { loadTossPayments } from '@tosspayments/payment-sdk'
import { getOrder, checkDeposit } from '../api/orders'
import { buildTossOrderId } from '../utils/toss'
import { useAuth } from '../context/AuthContext'

const fmt = (n) => n.toLocaleString('ko-KR') + '원'
const STATUS_LABEL = {
  ORDERED: '주문완료',
  WAITING_FOR_DEPOSIT: '입금대기',
  PAID: '결제완료',
  SHIPPING: '배송중',
  DELIVERED: '배송완료',
  CANCELED: '취소됨',
}
// 토스 테스트 가상계좌가 주로 내려주는 은행코드만 최소로 매핑. 목록에 없으면 코드 그대로 보여준다.
const BANK_NAME = { '20': '우리은행', '88': '신한은행', '81': '하나은행', '03': '기업은행' }

export default function OrderDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const [order, setOrder] = useState(null)
  const [paymentMethod, setPaymentMethod] = useState('카드')
  const [checking, setChecking] = useState(false)

  useEffect(() => {
    getOrder(id).then((res) => setOrder(res.data))
  }, [id])

  // "결제하기" 클릭 → 토스 결제창(팝업/리다이렉트) 오픈. 카드는 결제창에서 승인까지 끝나고,
  // 가상계좌는 "발급"만 끝난다 — 둘 다 successUrl로 리다이렉트되고, 이어지는 승인(confirm)
  // 요청은 PaymentSuccessPage에서 처리한다.
  const handlePay = async () => {
    try {
      const tossPayments = await loadTossPayments(import.meta.env.VITE_TOSS_CLIENT_KEY)
      const orderName =
        order.items.length > 1
          ? `${order.items[0].productName} 외 ${order.items.length - 1}건`
          : order.items[0].productName

      // customerEmail/customerName은 Toss SDK 내부에서 문자열 여부를 검사한다.
      // undefined를 넘기면 SDK가 .startsWith() 호출 시 TypeError를 던지므로
      // 값이 있을 때만 포함한다.
      const commonOptions = {
        amount: order.totalPrice,
        orderId: buildTossOrderId(order.id),
        orderName,
        successUrl: `${window.location.origin}/payments/success`,
        failUrl: `${window.location.origin}/payments/fail`,
        ...(user?.email && { customerEmail: user.email }),
        ...(user?.name && { customerName: user.name }),
      }

      await tossPayments.requestPayment(
        paymentMethod,
        paymentMethod === '가상계좌'
          ? { ...commonOptions, cashReceipt: { type: '미발행' } }
          : commonOptions,
      )
    } catch (err) {
      // 사용자가 결제창을 직접 닫은 경우(USER_CANCEL)는 굳이 에러로 알릴 필요 없다.
      if (err.code !== 'USER_CANCEL') {
        alert(err.message || '결제창을 여는 중 문제가 발생했습니다.')
      }
    }
  }

  // "입금 확인하기" 클릭 → 서버가 토스에 직접 물어봐서(폴링) 실제 입금 여부를 갱신한다.
  const handleCheckDeposit = async () => {
    setChecking(true)
    try {
      const res = await checkDeposit(order.id)
      setOrder(res.data)
      if (res.data.status === 'WAITING_FOR_DEPOSIT') {
        alert('아직 입금이 확인되지 않았습니다. 잠시 후 다시 시도해주세요.')
      }
    } catch {
      alert('입금 확인 중 문제가 발생했습니다. 잠시 후 다시 시도해주세요.')
    } finally {
      setChecking(false)
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
          color: 'var(--color-fg-muted)',
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
          borderBottom: '1px solid var(--color-border)',
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
          <p
            style={{
              margin: 0,
              fontSize: 13,
              letterSpacing: '0.14em',
              color: 'var(--color-fg-accent)',
            }}
          >
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
              color: 'var(--color-fg-muted)',
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
              borderBottom: '1px solid var(--color-fg)',
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
        <p
          style={{
            margin: '0 0 20px',
            fontSize: 13,
            color: 'var(--color-fg-muted)',
            fontWeight: 300,
          }}
        >
          주문일시: {new Date(order.createdAt).toLocaleString()}
        </p>

        {/* 배송지 정보 — order에 deliveryAddress가 있을 때만 표시 */}
        {order.deliveryAddress && (
          <div
            style={{
              background: 'var(--color-bg-hover-light)',
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
              <p style={{ margin: 0, color: 'var(--color-fg-muted)' }}>
                메모: {order.deliveryNote}
              </p>
            )}
          </div>
        )}

        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            borderTop: '1px solid var(--color-border)',
          }}
        >
          {order.items?.map((item, i) => (
            <div
              key={i}
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                padding: '14px 0',
                borderBottom: '1px solid var(--color-border)',
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

        {/* 결제대기 상태에서만 결제수단 선택 + 결제 버튼 노출 */}
        {order.status === 'ORDERED' && (
          <>
            <div style={{ display: 'flex', gap: 20, marginBottom: 16, fontSize: 14 }}>
              {['카드', '가상계좌'].map((m) => (
                <label key={m} style={{ display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer' }}>
                  <input
                    type="radio"
                    name="paymentMethod"
                    value={m}
                    checked={paymentMethod === m}
                    onChange={() => setPaymentMethod(m)}
                  />
                  {m === '가상계좌' ? '무통장입금(가상계좌)' : m}
                </label>
              ))}
            </div>
            <button
              onClick={handlePay}
              style={{
                cursor: 'pointer',
                border: '1px solid var(--color-fg)',
                background: 'var(--color-fg)',
                color: 'var(--color-bg)',
                padding: '16px 22px',
                fontSize: 14,
                letterSpacing: '0.04em',
                width: '100%',
                marginBottom: 24,
              }}
            >
              결제하기
            </button>
          </>
        )}

        {/* 가상계좌 발급 후 입금 대기 중 — 계좌정보 표시 + 직접 입금 확인 */}
        {order.status === 'WAITING_FOR_DEPOSIT' && order.payment && (
          <div
            style={{
              background: 'var(--color-bg-hover-light)',
              padding: '20px 22px',
              marginBottom: 24,
              display: 'flex',
              flexDirection: 'column',
              gap: 10,
              fontSize: 13,
              fontWeight: 300,
              lineHeight: 1.8,
            }}
          >
            <p style={{ margin: 0, fontWeight: 400 }}>입금 계좌 안내</p>
            <p style={{ margin: 0 }}>
              {BANK_NAME[order.payment.virtualAccountBankCode] ?? order.payment.virtualAccountBankCode}{' '}
              {order.payment.virtualAccountNumber}
            </p>
            {order.payment.virtualAccountDueDate && (
              <p style={{ margin: 0, color: 'var(--color-fg-muted)' }}>
                입금기한: {new Date(order.payment.virtualAccountDueDate).toLocaleString()}
              </p>
            )}
            <button
              onClick={handleCheckDeposit}
              disabled={checking}
              style={{
                cursor: checking ? 'default' : 'pointer',
                border: '1px solid var(--color-fg)',
                background: 'var(--color-bg)',
                color: 'var(--color-fg)',
                padding: '10px 16px',
                fontSize: 13,
                width: 'fit-content',
                marginTop: 4,
              }}
            >
              {checking ? '확인 중...' : '입금 확인하기'}
            </button>
          </div>
        )}

        <div style={{ display: 'flex', gap: 24 }}>
          <span
            onClick={() => navigate('/orders')}
            style={{
              cursor: 'pointer',
              fontSize: 13,
              color: 'var(--color-fg-muted)',
              borderBottom: '1px solid var(--color-border)',
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
              color: 'var(--color-fg-muted)',
              borderBottom: '1px solid var(--color-border)',
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
