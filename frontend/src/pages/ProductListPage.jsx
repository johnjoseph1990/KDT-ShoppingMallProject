import { useEffect, useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { getProducts } from '../api/products'
import { addToCart } from '../api/cart'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'

const fmt = (n) => n.toLocaleString('ko-KR') + '원'

/* 카테고리 필터 → 키워드 검색으로 매핑 */
const CATS = [
  { key: 'all', label: '전체', keyword: '' },
  { key: 'veg', label: '채소·과일', keyword: '채소' },
  { key: 'bake', label: '베이커리·간식', keyword: '베이커리' },
  { key: 'box', label: '꾸러미·정기배송', keyword: '꾸러미' },
]

export default function ProductListPage() {
  const [products, setProducts] = useState([])
  const [cat, setCat] = useState('all')
  const [page, setPage] = useState(0)
  const navigate = useNavigate()
  // useLocation(): 현재 페이지 경로 정보를 담고 있음. 로그인 후 "원래 있던 페이지"로
  // 돌아오게 하려면, 로그인으로 넘어가기 전에 이 위치 정보를 함께 들고 가야 한다.
  const location = useLocation()
  const { user } = useAuth()
  const { refreshCart, showToast } = useCart()

  useEffect(() => {
    const keyword = CATS.find((c) => c.key === cat)?.keyword || ''
    getProducts({ ...(keyword ? { keyword } : {}), page })
      .then((res) => setProducts(res.data.content || []))
      .catch(() => {})
  }, [cat, page])

  const handleCatChange = (key) => {
    setCat(key)
    setPage(0)
  }

  const handleAddToCart = async (e, product) => {
    e.stopPropagation()
    if (!user) {
      alert('로그인 후 담을 수 있어요')
      // state: { from: location } — 로그인 페이지에 "로그인 성공하면 여기로 돌아와야 해"라고 알려주는 값
      navigate('/login', { state: { from: location } })
      return
    }
    try {
      await addToCart({ productId: product.id, quantity: 1 })
      refreshCart()
      showToast(product.name)
    } catch {}
  }

  return (
    <main
      style={{
        animation: 'fadeUp .4s ease both',
        padding: 'clamp(32px,5vw,64px) clamp(20px,5vw,72px)',
        flex: 1,
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
        쇼핑
      </h1>
      <p style={{ margin: '0 0 36px', fontSize: 14, color: '#6d6c61', fontWeight: 300 }}>
        {products.length}개의 상품 · 매주 화·금 수확분 기준
      </p>

      {/* 카테고리 필터 버튼 */}
      <div style={{ display: 'flex', gap: 10, marginBottom: 36, flexWrap: 'wrap' }}>
        {CATS.map((c) => (
          <CatBtn key={c.key} active={cat === c.key} onClick={() => handleCatChange(c.key)}>
            {c.label}
          </CatBtn>
        ))}
      </div>

      {/* 3열 상품 그리드 */}
      {products.length === 0 ? (
        <p style={{ fontSize: 14, color: '#6d6c61', fontWeight: 300 }}>상품이 없습니다.</p>
      ) : (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(3,1fr)',
            gap: 1,
            background: '#dddaca',
            border: '1px solid #dddaca',
          }}
        >
          {products.map((p) => (
            <ProductCard
              key={p.id}
              product={p}
              onOpen={() => navigate(`/products/${p.id}`)}
              onAdd={(e) => handleAddToCart(e, p)}
            />
          ))}
        </div>
      )}

      {/* 페이지네이션 */}
      <div
        style={{
          display: 'flex',
          gap: 12,
          marginTop: 40,
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <PageBtn onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0}>
          ← 이전
        </PageBtn>
        <span style={{ fontSize: 13, color: '#6d6c61', fontWeight: 300 }}>페이지 {page + 1}</span>
        <PageBtn onClick={() => setPage((p) => p + 1)} disabled={products.length < 10}>
          다음 →
        </PageBtn>
      </div>
    </main>
  )
}

function CatBtn({ active, onClick, children }) {
  return (
    <button
      onClick={onClick}
      style={{
        cursor: 'pointer',
        border: `1px solid ${active ? '#333330' : '#dddaca'}`,
        background: active ? '#333330' : 'transparent',
        color: active ? '#fffef2' : '#333330',
        padding: '9px 18px',
        fontSize: 13,
        letterSpacing: '0.03em',
        fontWeight: 300,
      }}
    >
      {children}
    </button>
  )
}

function ProductCard({ product, onOpen, onAdd }) {
  const [hovered, setHovered] = useState(false)
  return (
    <div
      style={{
        background: hovered ? '#f6f4e6' : '#fffef2',
        padding: 28,
        display: 'flex',
        flexDirection: 'column',
        gap: 16,
      }}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
    >
      {/* 상품 이미지 */}
      <div
        onClick={onOpen}
        style={{
          cursor: 'pointer',
          aspectRatio: '4/5',
          background: '#edeadb',
          overflow: 'hidden',
        }}
      >
        {product.imageUrl && (
          <img
            src={product.imageUrl}
            alt={product.name}
            style={{ width: '100%', height: '100%', objectFit: 'cover' }}
          />
        )}
      </div>

      {/* 상품 정보 */}
      <div
        onClick={onOpen}
        style={{ cursor: 'pointer', display: 'flex', flexDirection: 'column', gap: 6, flex: 1 }}
      >
        <h3
          style={{
            margin: 0,
            fontFamily: "'Noto Serif KR', serif",
            fontWeight: 400,
            fontSize: 17,
          }}
        >
          {product.name}
        </h3>
        <p style={{ margin: 0, fontSize: 13, color: '#6d6c61', fontWeight: 300, lineHeight: 1.7 }}>
          {product.description?.slice(0, 40)}
        </p>
        <p style={{ margin: '6px 0 0', fontSize: 14 }}>{fmt(product.price)}</p>
      </div>

      {/* 장바구니 버튼 */}
      <button
        onClick={onAdd}
        style={{
          cursor: 'pointer',
          border: '1px solid #333330',
          background: 'transparent',
          color: '#333330',
          padding: '12px',
          fontSize: 13,
          letterSpacing: '0.04em',
        }}
        onMouseEnter={(e) => {
          e.currentTarget.style.background = '#333330'
          e.currentTarget.style.color = '#fffef2'
        }}
        onMouseLeave={(e) => {
          e.currentTarget.style.background = 'transparent'
          e.currentTarget.style.color = '#333330'
        }}
      >
        장바구니에 담기
      </button>
    </div>
  )
}

function PageBtn({ onClick, disabled, children }) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      style={{
        cursor: disabled ? 'default' : 'pointer',
        border: '1px solid #dddaca',
        background: 'transparent',
        color: '#333330',
        padding: '8px 16px',
        fontSize: 13,
        opacity: disabled ? 0.4 : 1,
      }}
    >
      {children}
    </button>
  )
}
