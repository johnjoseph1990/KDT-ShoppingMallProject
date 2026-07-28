import { createContext, useContext, useCallback, useEffect, useRef, useState } from 'react'
import { useAuth } from './AuthContext'
import { getCart, updateCartItem, removeCartItem } from '../api/cart'

/* 장바구니 상태(아이템, 드로어, 토스트)를 앱 전체에서 공유하는 컨텍스트 */
const CartContext = createContext(null)

export function CartProvider({ children }) {
  const { user } = useAuth()
  const [cartItems, setCartItems] = useState([])
  const [cartOpen, setCartOpen] = useState(false)
  // toast: null | { text: string, type: 'success' | 'error' }
  const [toast, setToast] = useState(null)
  const toastTimer = useRef(null)

  /* 서버에서 장바구니 목록을 다시 불러온다 */
  const refreshCart = useCallback(() => {
    if (!user) {
      setCartItems([])
      return
    }
    getCart()
      .then((res) => setCartItems(res.data))
      .catch(() => {})
  }, [user])

  /* 로그인 상태가 바뀔 때마다 장바구니 갱신 */
  useEffect(() => {
    refreshCart()
  }, [refreshCart])

  const openCart = () => setCartOpen(true)
  const closeCart = () => setCartOpen(false)

  /* 장바구니 담기 성공 토스트 — 상품명을 받아 "○○ — 장바구니에 담았습니다" 표시 */
  const showToast = (name) => {
    setToast({ text: `${name} — 장바구니에 담았습니다`, type: 'success' })
    clearTimeout(toastTimer.current)
    toastTimer.current = setTimeout(() => setToast(null), 2200)
  }

  /* 에러·안내 메시지 토스트 — 전달받은 문자열을 그대로 표시 */
  const showMessage = (text) => {
    setToast({ text, type: 'error' })
    clearTimeout(toastTimer.current)
    toastTimer.current = setTimeout(() => setToast(null), 2200)
  }

  /* 장바구니 수량 변경 (0 이하면 삭제) */
  const changeQty = async (cartItemId, quantity) => {
    if (quantity < 1) {
      await removeCartItem(cartItemId).catch(() => {})
    } else {
      await updateCartItem(cartItemId, { quantity }).catch(() => {})
    }
    refreshCart()
  }

  const removeItem = async (cartItemId) => {
    await removeCartItem(cartItemId).catch(() => {})
    refreshCart()
  }

  const cartCount = cartItems.reduce((t, i) => t + i.quantity, 0)
  const cartTotal = cartItems.reduce((t, i) => t + i.price * i.quantity, 0)

  return (
    <CartContext.Provider
      value={{
        cartItems,
        cartOpen,
        toast,
        cartCount,
        cartTotal,
        refreshCart,
        openCart,
        closeCart,
        showToast,
        showMessage,
        changeQty,
        removeItem,
      }}
    >
      {children}
    </CartContext.Provider>
  )
}

export const useCart = () => useContext(CartContext)
