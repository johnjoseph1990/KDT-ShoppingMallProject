import { useCart } from '../context/CartContext'

/* 장바구니 담기 완료 시 하단에 표시되는 토스트 메시지 */
export default function Toast() {
  const { toast } = useCart()
  if (!toast) return null

  return (
    <div
      style={{
        position: 'fixed',
        bottom: 28,
        left: '50%',
        transform: 'translateX(-50%)',
        background: '#333326',
        color: '#e5e3d3',
        padding: '13px 26px',
        fontSize: 13,
        letterSpacing: '0.03em',
        zIndex: 120,
        animation: 'fadeUp .3s ease both',
        whiteSpace: 'nowrap',
      }}
    >
      {toast} — 장바구니에 담았습니다
    </div>
  )
}
