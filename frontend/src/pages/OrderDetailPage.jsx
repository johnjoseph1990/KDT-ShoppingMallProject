import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { loadTossPayments } from '@tosspayments/payment-sdk'
import { getOrder, checkDeposit } from '../api/orders'
import { buildTossOrderId } from '../utils/toss'
import { useAuth } from '../context/AuthContext'
// 주문 상태 한글 표기는 utils/orderStatus 한 곳에서 관리한다
import { orderStatusLabel } from '../utils/orderStatus'
import TextLink from '../components/TextLink'

const fmt = (n) => n.toLocaleString('ko-KR') + '원'
// 토스 테스트 가상계좌가 주로 내려주는 은행코드만 최소로 매핑. 목록에 없으면 코드 그대로 보여준다.
const BANK_NAME = { 20: '우리은행', 88: '신한은행', 81: '하나은행', '03': '기업은행' }

// 결제수단 정의를 한 곳에 모아둔다. 핵심은 value와 label을 분리한 것:
//   value  — 토스 SDK requestPayment()의 첫 인자로 그대로 넘어가는 값이라 절대 바꿀 수 없다(외부 계약).
//   label  — 화면에 보여줄 이름. 사용자 눈높이로 자유롭게 다듬을 수 있다.
// description은 "이걸 고르면 내 주문이 어떻게 되는가"를 알려주는 문구다. 사용자가 알고 싶은 건
// 수단 이름이 아니라 결과(언제 발송되는지)이므로, 이 한 줄이 선택 UI의 핵심이다.
// payLabel은 결제 버튼 문구다. 카드는 승인까지 끝나지만 가상계좌는 "계좌 발급"만 되므로,
// 두 경우에 같은 "결제하기"를 쓰면 사용자가 결제가 끝난 줄 착각한다. 총액이 필요해 함수로 받는다.
const PAYMENT_METHODS = [
  {
    value: '카드',
    label: '신용·체크카드',
    description: '결제 즉시 발송 준비가 시작됩니다',
    payLabel: (total) => `${fmt(total)} 결제하기`,
  },
  {
    value: '가상계좌',
    label: '무통장입금(가상계좌)',
    // 카드와 결정적으로 다른 점(입금 후 발송)만 담고, 자동 취소 안내는 계좌 발급 후
    // WAITING_FOR_DEPOSIT 화면의 "입금기한" 표시로 미룬다 — 선택 직전에 취소를 언급하지 않는다.
    description: '입금이 확인된 뒤 발송됩니다',
    payLabel: () => '입금 계좌 발급받기',
  },
]

export default function OrderDetailPage() {
  const { id } = useParams()
  // 이동은 전부 TextLink(<a>)가 담당하므로 useNavigate는 더 이상 필요 없다 (DEF-7)
  const { user } = useAuth()
  const [order, setOrder] = useState(null)
  const [paymentMethod, setPaymentMethod] = useState('카드')
  const [checking, setChecking] = useState(false)

  // 현재 선택된 결제수단 객체를 찾아둔다. 아래 결제 버튼 문구를 만들 때 사용한다.
  // find()는 조건에 맞는 첫 요소를 돌려준다 — paymentMethod는 항상 둘 중 하나라 없을 수 없다.
  const selectedMethod = PAYMENT_METHODS.find((m) => m.value === paymentMethod)

  useEffect(() => {
    getOrder(id).then((res) => setOrder(res.data))
  }, [id])

  // "결제하기" 클릭 → 토스 결제창(팝업/리다이렉트) 오픈. 카드는 결제창에서 승인까지 끝나고,
  // 가상계좌는 "발급"만 끝난다 — 둘 다 successUrl로 리다이렉트되고, 이어지는 승인(confirm)
  // 요청은 PaymentSuccessPage에서 처리한다.
  const handlePay = async () => {
    // VITE_TOSS_CLIENT_KEY가 없으면 SDK가 내부적으로 undefined.startsWith()를 호출해 TypeError 발생
    const clientKey = import.meta.env.VITE_TOSS_CLIENT_KEY
    if (!clientKey) {
      alert('결제 키가 설정되지 않았습니다. frontend/.env에 VITE_TOSS_CLIENT_KEY를 추가하세요.')
      return
    }
    try {
      const tossPayments = await loadTossPayments(clientKey)
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
            주문 #{order.id} · {orderStatusLabel(order.status)}
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
          {/* 이동이므로 진짜 링크로 (DEF-7) */}
          <TextLink
            to="/"
            style={{
              fontSize: 13,
              letterSpacing: '0.05em',
              borderBottom: '1px solid var(--color-fg)',
              paddingBottom: 2,
            }}
          >
            처음으로 돌아가기
          </TextLink>
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

        {/* 배송비 별도 표기 — order.totalPrice에는 이미 배송비가 합산돼 있으므로,
            그 안에서 배송비만큼을 빼서 보여주면 장바구니 화면과 동일한 형태(상품금액+배송비)가 된다.
            이전엔 장바구니에서 보여준 합계와 실제 주문 금액이 달라 혼란을 주는 결함(DEF-2)이 있었다. */}
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            padding: '10px 0',
            fontSize: 13,
            color: 'var(--color-fg-muted)',
          }}
        >
          <span>배송비</span>
          <span>{order.shippingFee > 0 ? fmt(order.shippingFee) : '무료'}</span>
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
            {/* fieldset/legend: 여러 입력을 "하나의 질문에 대한 선택지"로 묶는 표준 HTML 방식이다.
                스크린리더가 각 항목을 읽을 때 "결제수단" 그룹임을 함께 알려준다.
                기본 테두리·여백은 사이트 톤과 맞지 않아 none/0으로 지운다. */}
            <fieldset style={{ border: 'none', margin: '0 0 16px', padding: 0 }}>
              <legend
                style={{
                  padding: 0,
                  marginBottom: 10,
                  fontSize: 13,
                  color: 'var(--color-fg-muted)',
                }}
              >
                결제수단
              </legend>

              {/* 가로 인라인 라디오는 모바일에서 터치 영역이 라디오 점과 짧은 글자뿐이었다.
                  테두리 있는 카드를 세로로 쌓아 행 전체를 누를 수 있게 한다. */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                {PAYMENT_METHODS.map((m) => {
                  // 선택 여부에 따라 테두리 진하기를 바꿔 "지금 이게 선택됨"을 눈으로 알 수 있게 한다.
                  const isSelected = paymentMethod === m.value
                  return (
                    // label이 input을 감싸면 카드 어디를 눌러도 선택된다(암묵적 라벨 연결).
                    // htmlFor/id를 따로 붙이지 않아도 되고, 클릭 영역이 카드 전체로 넓어진다.
                    <label
                      key={m.value}
                      style={{
                        display: 'flex',
                        alignItems: 'flex-start',
                        gap: 10,
                        cursor: 'pointer',
                        padding: '14px 16px',
                        border: `1px solid ${isSelected ? 'var(--color-fg)' : 'var(--color-border)'}`,
                        background: isSelected ? 'var(--color-bg-hover-light)' : 'transparent',
                      }}
                    >
                      <input
                        type="radio"
                        // name이 같은 라디오끼리 한 그룹이 되어 "하나만 선택" 동작이 만들어진다.
                        name="paymentMethod"
                        // SDK에 넘어가는 값(value)과 화면 표시(label)를 분리한 지점
                        value={m.value}
                        checked={isSelected}
                        onChange={() => setPaymentMethod(m.value)}
                        style={{ marginTop: 3, cursor: 'pointer' }}
                      />
                      <span style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                        <span style={{ fontSize: 14 }}>{m.label}</span>
                        {/* 결과 안내 문구 — 수단 이름보다 이 줄이 선택을 돕는다 */}
                        <span
                          style={{
                            fontSize: 12.5,
                            fontWeight: 300,
                            lineHeight: 1.6,
                            color: 'var(--color-fg-muted)',
                          }}
                        >
                          {m.description}
                        </span>
                      </span>
                    </label>
                  )
                })}
              </div>
            </fieldset>
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
              {/* 선택한 수단에 따라 문구가 달라진다 — 가상계좌는 이 버튼으로 "발급"만 되기 때문 */}
              {selectedMethod.payLabel(order.totalPrice)}
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
              {BANK_NAME[order.payment.virtualAccountBankCode] ??
                order.payment.virtualAccountBankCode}{' '}
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
          <TextLink
            to="/orders"
            style={{
              fontSize: 13,
              color: 'var(--color-fg-muted)',
              borderBottom: '1px solid var(--color-border)',
              paddingBottom: 1,
            }}
          >
            주문 목록
          </TextLink>
          <TextLink
            to="/shop"
            style={{
              fontSize: 13,
              color: 'var(--color-fg-muted)',
              borderBottom: '1px solid var(--color-border)',
              paddingBottom: 1,
            }}
          >
            계속 쇼핑하기
          </TextLink>
        </div>
      </section>
    </main>
  )
}
