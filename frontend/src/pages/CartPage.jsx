import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, IconButton } from '@vapor-ui/core'
import { getCart, updateCartItem, removeCartItem } from '../api/cart'
import { createOrder } from '../api/orders'

export default function CartPage() {
  const [items, setItems] = useState([])
  const navigate = useNavigate()

  useEffect(() => {
    loadCart()
  }, [])

  const loadCart = () => {
    getCart().then((res) => setItems(res.data))
  }

  // 수량 변경: 즉시 서버에 반영
  const handleQtyChange = async (cartItemId, quantity) => {
    if (quantity < 1) return
    try {
      await updateCartItem(cartItemId, { quantity })
      loadCart()
    } catch (err) {
      alert(err.response?.data?.message || '수량 변경 실패')
    }
  }

  const handleRemove = async (cartItemId) => {
    await removeCartItem(cartItemId)
    loadCart()
  }

  // 선택된 장바구니 항목으로 주문 생성
  const handleOrder = async () => {
    if (items.length === 0) return
    try {
      const orderItems = items.map((i) => ({ cartItemId: i.id }))
      const res = await createOrder({ items: orderItems })
      navigate(`/orders/${res.data.id}`)
    } catch (err) {
      alert(err.response?.data?.message || '주문 실패')
    }
  }

  const totalPrice = items.reduce((sum, i) => sum + i.price * i.quantity, 0)

  return (
    <div style={{ maxWidth: '600px', margin: '0 auto' }}>
      <h2>장바구니</h2>
      {items.length === 0 ? (
        <p>장바구니가 비어있습니다.</p>
      ) : (
        <>
          {items.map((item) => (
            <div key={item.id} style={styles.row}>
              <div style={{ flex: 1 }}>
                <strong>{item.productName}</strong>
                <p style={{ margin: '0.2rem 0', color: '#555' }}>{item.price.toLocaleString()}원</p>
              </div>
              <div style={styles.qtyRow}>
                <IconButton
                  onClick={() => handleQtyChange(item.id, item.quantity - 1)}
                  variant="outline"
                  size="sm"
                  aria-label="수량 감소"
                >
                  -
                </IconButton>
                <span style={{ padding: '0 0.5rem' }}>{item.quantity}</span>
                <IconButton
                  onClick={() => handleQtyChange(item.id, item.quantity + 1)}
                  variant="outline"
                  size="sm"
                  aria-label="수량 증가"
                >
                  +
                </IconButton>
              </div>
              <span style={styles.subtotal}>{(item.price * item.quantity).toLocaleString()}원</span>
              <IconButton
                onClick={() => handleRemove(item.id)}
                variant="ghost"
                colorPalette="danger"
                aria-label="삭제"
              >
                ✕
              </IconButton>
            </div>
          ))}

          <div style={styles.total}>
            <strong>합계: {totalPrice.toLocaleString()}원</strong>
          </div>
          <Button onClick={handleOrder} colorPalette="primary" style={{ width: '100%' }}>
            주문하기
          </Button>
        </>
      )}
    </div>
  )
}

const styles = {
  row: {
    display: 'flex',
    alignItems: 'center',
    gap: '1rem',
    padding: '0.75rem 0',
    borderBottom: '1px solid #eee',
  },
  qtyRow: { display: 'flex', alignItems: 'center' },
  subtotal: { minWidth: '80px', textAlign: 'right', fontWeight: 'bold' },
  total: { textAlign: 'right', padding: '1rem 0', fontSize: '1.1rem' },
}
