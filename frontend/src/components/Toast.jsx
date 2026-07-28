import { useCart } from '../context/CartContext'

/* 장바구니 성공·에러 안내 토스트. toast = { text, type: 'success' | 'error' } */
export default function Toast() {
  const { toast } = useCart()
  if (!toast) return null

  // 에러는 붉은 계열, 성공은 기존 다크 배경
  const bg = toast.type === 'error' ? 'var(--color-danger-dark)' : 'var(--color-bg-dark)'

  return (
    <div
      style={{
        position: 'fixed',
        bottom: 28,
        left: '50%',
        transform: 'translateX(-50%)',
        background: bg,
        color: 'var(--color-text-on-dark)',
        padding: '13px 26px',
        fontSize: 13,
        letterSpacing: '0.03em',
        zIndex: 120,
        animation: 'fadeUp .3s ease both',
        whiteSpace: 'nowrap',
      }}
    >
      {toast.text}
    </div>
  )
}
