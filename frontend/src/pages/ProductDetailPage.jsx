import { useEffect, useState } from 'react'
import { useParams, useNavigate, useLocation } from 'react-router-dom'
import { getProduct, getRecommendations } from '../api/products'
import { addToCart } from '../api/cart'
import { getReviews, createReview, deleteReview } from '../api/reviews'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'

const fmt = (n) => n.toLocaleString('ko-KR') + '원'

export default function ProductDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const { user } = useAuth()
  const { refreshCart, showToast } = useCart()

  const [product, setProduct] = useState(null)
  const [reviews, setReviews] = useState([])
  // 같은 태그를 가진 다른 상품 추천 목록 (R-6)
  const [recommendations, setRecommendations] = useState([])
  const [quantity, setQuantity] = useState(1)
  const [reviewForm, setReviewForm] = useState({ rating: 5, content: '' })

  useEffect(() => {
    getProduct(id).then((res) => setProduct(res.data))
    // 추천 상품 조회 (태그가 없는 상품이면 빈 배열이 돌아옴)
    getRecommendations(id)
      .then((res) => setRecommendations(res.data))
      .catch(() => {})
    loadReviews()
  }, [id])

  const loadReviews = () => {
    getReviews(id)
      .then((res) => setReviews(res.data))
      .catch(() => {})
  }

  const handleAddToCart = async () => {
    if (!user) {
      alert('로그인 후 담을 수 있어요')
      navigate('/login', { state: { from: location } })
      return
    }
    try {
      await addToCart({ productId: Number(id), quantity })
      refreshCart()
      showToast(product.name)
    } catch (err) {
      alert(err.response?.data?.message || '장바구니 추가 실패')
    }
  }

  const handleReviewSubmit = async (e) => {
    e.preventDefault()
    try {
      await createReview(id, reviewForm)
      setReviewForm({ rating: 5, content: '' })
      loadReviews()
    } catch (err) {
      alert(err.response?.data?.message || '리뷰 작성 실패')
    }
  }

  const handleDeleteReview = async (reviewId) => {
    if (!confirm('리뷰를 삭제하시겠습니까?')) return
    try {
      await deleteReview(id, reviewId)
      loadReviews()
    } catch {
      alert('삭제 권한이 없습니다.')
    }
  }

  if (!product)
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
      {/* ─── 2단 레이아웃: 왼쪽 스티키 이미지 / 오른쪽 정보 ───
          mobile-1col: 모바일에서는 위아래로 쌓아 좁은 화면에서도 정보/버튼이 안 잘리게 함 */}
      <div
        className="mobile-1col"
        style={{
          display: 'grid',
          gridTemplateColumns: '1.1fr 1fr',
          borderBottom: '1px solid #dddaca',
          minHeight: '76vh',
        }}
      >
        {/* 왼쪽: 스크롤 시 뷰포트에 고정되는 상품 이미지.
            hero-sticky-image: 모바일에서는 고정 해제 + 높이 축소 (화면 독차지 방지) */}
        <div
          className="hero-sticky-image"
          style={{
            background: '#edeadb',
            position: 'sticky',
            top: 72,
            height: 'calc(100vh - 72px)',
            overflow: 'hidden',
          }}
        >
          {product.imageUrl ? (
            <img
              src={product.imageUrl}
              alt={product.name}
              style={{ width: '100%', height: '100%', objectFit: 'cover' }}
            />
          ) : (
            <div style={{ width: '100%', height: '100%', background: '#edeadb' }} />
          )}
        </div>

        {/* 오른쪽: 상품 상세 정보 */}
        <div
          style={{
            padding: 'clamp(32px,5vw,72px)',
            display: 'flex',
            flexDirection: 'column',
            gap: 28,
          }}
        >
          <span
            onClick={() => navigate('/shop')}
            style={{ cursor: 'pointer', fontSize: 13, color: '#6d6c61' }}
          >
            ← 쇼핑으로 돌아가기
          </span>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {product.tags?.length > 0 && (
              <p
                style={{
                  margin: 0,
                  fontSize: 12,
                  letterSpacing: '0.14em',
                  color: '#75775e',
                }}
              >
                {product.tags.join(' · ')}
              </p>
            )}
            <h1
              style={{
                margin: 0,
                fontFamily: "'Noto Serif KR', serif",
                fontWeight: 300,
                fontSize: 'clamp(26px,2.6vw,36px)',
                lineHeight: 1.4,
              }}
            >
              {product.name}
            </h1>
            <p
              style={{
                margin: 0,
                fontSize: 15,
                color: '#6d6c61',
                fontWeight: 300,
                lineHeight: 1.9,
              }}
            >
              {product.description}
            </p>
          </div>

          {/* 재고 상태 */}
          <p
            style={{
              margin: 0,
              fontSize: 14,
              color: product.stockQuantity > 0 ? '#75775e' : '#e63946',
            }}
          >
            {product.stockQuantity > 0 ? `재고 ${product.stockQuantity}개` : '품절'}
          </p>

          {/* 수량 스테퍼 + 장바구니 버튼 */}
          <div style={{ display: 'flex', gap: 14, alignItems: 'stretch' }}>
            <div style={{ display: 'flex', border: '1px solid #333330' }}>
              <button
                onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                style={{
                  cursor: 'pointer',
                  border: 'none',
                  background: 'transparent',
                  width: 44,
                  fontSize: 16,
                }}
              >
                −
              </button>
              <span
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  width: 40,
                  fontSize: 14,
                }}
              >
                {quantity}
              </span>
              <button
                onClick={() => setQuantity((q) => Math.min(product.stockQuantity || q + 1, q + 1))}
                disabled={product.stockQuantity === 0}
                style={{
                  cursor: 'pointer',
                  border: 'none',
                  background: 'transparent',
                  width: 44,
                  fontSize: 16,
                }}
              >
                +
              </button>
            </div>
            <AddCartBtn
              onClick={handleAddToCart}
              disabled={product.stockQuantity === 0}
              price={fmt(product.price)}
            />
          </div>
        </div>
      </div>

      {/* ─── 같은 태그 추천 상품 섹션 (R-6) ─── */}
      {recommendations.length > 0 && (
        <section
          style={{
            padding: 'clamp(40px,5vw,72px) clamp(20px,5vw,72px) 0',
          }}
        >
          <h2
            style={{
              margin: '0 0 24px',
              fontFamily: "'Noto Serif KR', serif",
              fontWeight: 300,
              fontSize: 24,
            }}
          >
            이런 상품은 어떠세요?
          </h2>
          <div
            className="mobile-2col"
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(4,1fr)',
              gap: 1,
              background: '#dddaca',
              border: '1px solid #dddaca',
            }}
          >
            {recommendations.map((r) => (
              <div
                key={r.id}
                onClick={() => navigate(`/products/${r.id}`)}
                style={{ background: '#fffef2', padding: 20, cursor: 'pointer' }}
              >
                <div
                  style={{
                    aspectRatio: '4/5',
                    background: '#edeadb',
                    overflow: 'hidden',
                    marginBottom: 12,
                  }}
                >
                  {r.imageUrl && (
                    <img
                      src={r.imageUrl}
                      alt={r.name}
                      style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                    />
                  )}
                </div>
                <p style={{ margin: '0 0 4px', fontSize: 14 }}>{r.name}</p>
                <p style={{ margin: 0, fontSize: 13, color: '#6d6c61' }}>{fmt(r.price)}</p>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* ─── 리뷰 섹션 ─── */}
      <section style={{ padding: 'clamp(40px,5vw,72px) clamp(20px,5vw,72px)' }}>
        <h2
          style={{
            margin: '0 0 32px',
            fontFamily: "'Noto Serif KR', serif",
            fontWeight: 300,
            fontSize: 24,
          }}
        >
          리뷰 ({reviews.length})
        </h2>

        {reviews.length === 0 ? (
          <p style={{ fontSize: 14, color: '#6d6c61', fontWeight: 300 }}>첫 리뷰를 작성해보세요.</p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', borderTop: '1px solid #dddaca' }}>
            {reviews.map((r) => (
              <div key={r.id} style={{ padding: '24px 0', borderBottom: '1px solid #dddaca' }}>
                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    marginBottom: 8,
                  }}
                >
                  <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
                    <strong style={{ fontSize: 14 }}>{r.memberName}</strong>
                    <span style={{ fontSize: 13, color: '#75775e' }}>
                      {'★'.repeat(r.rating)}
                      {'☆'.repeat(5 - r.rating)}
                    </span>
                    <span style={{ fontSize: 12, color: '#6d6c61', fontWeight: 300 }}>
                      {new Date(r.createdAt).toLocaleDateString()}
                    </span>
                  </div>
                  {user && user.id === r.memberId && (
                    <button
                      onClick={() => handleDeleteReview(r.id)}
                      style={{
                        cursor: 'pointer',
                        border: 'none',
                        background: 'transparent',
                        fontSize: 12,
                        color: '#6d6c61',
                      }}
                    >
                      삭제
                    </button>
                  )}
                </div>
                <p
                  style={{
                    margin: 0,
                    fontSize: 14,
                    lineHeight: 1.8,
                    fontWeight: 300,
                    color: '#4a4a3a',
                  }}
                >
                  {r.content}
                </p>
              </div>
            ))}
          </div>
        )}

        {/* 리뷰 작성 폼 (로그인한 경우만) */}
        {user && (
          <form
            onSubmit={handleReviewSubmit}
            style={{
              marginTop: 40,
              display: 'flex',
              flexDirection: 'column',
              gap: 16,
              maxWidth: 480,
            }}
          >
            <h3
              style={{
                margin: 0,
                fontFamily: "'Noto Serif KR', serif",
                fontWeight: 300,
                fontSize: 18,
              }}
            >
              리뷰 작성
            </h3>
            <label
              style={{
                fontSize: 13,
                color: '#6d6c61',
                display: 'flex',
                flexDirection: 'column',
                gap: 8,
              }}
            >
              별점
              <select
                value={reviewForm.rating}
                onChange={(e) => setReviewForm({ ...reviewForm, rating: Number(e.target.value) })}
                style={{
                  border: '1px solid #dddaca',
                  background: 'transparent',
                  padding: '10px 14px',
                  fontSize: 14,
                  outline: 'none',
                }}
              >
                {[5, 4, 3, 2, 1].map((v) => (
                  <option key={v} value={v}>
                    {v}점
                  </option>
                ))}
              </select>
            </label>
            <label
              style={{
                fontSize: 13,
                color: '#6d6c61',
                display: 'flex',
                flexDirection: 'column',
                gap: 8,
              }}
            >
              내용
              <textarea
                value={reviewForm.content}
                onChange={(e) => setReviewForm({ ...reviewForm, content: e.target.value })}
                placeholder="리뷰 내용을 입력하세요"
                rows={4}
                required
                style={{
                  border: '1px solid #dddaca',
                  background: 'transparent',
                  padding: '14px',
                  fontSize: 14,
                  outline: 'none',
                  resize: 'vertical',
                  fontWeight: 300,
                }}
              />
            </label>
            <button
              type="submit"
              style={{
                cursor: 'pointer',
                border: '1px solid #333330',
                background: '#333330',
                color: '#fffef2',
                padding: '14px 22px',
                fontSize: 13,
                letterSpacing: '0.04em',
                alignSelf: 'flex-start',
              }}
            >
              리뷰 등록
            </button>
          </form>
        )}
      </section>
    </main>
  )
}

/* 장바구니 담기 버튼 (hover 시 색상 변경) */
function AddCartBtn({ onClick, disabled, price }) {
  const [hovered, setHovered] = useState(false)
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      onMouseEnter={() => !disabled && setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        cursor: disabled ? 'default' : 'pointer',
        flex: 1,
        border: `1px solid ${hovered ? '#75775e' : '#333330'}`,
        background: hovered ? '#75775e' : '#333330',
        color: '#fffef2',
        padding: '16px 22px',
        fontSize: 14,
        letterSpacing: '0.04em',
        display: 'flex',
        justifyContent: 'space-between',
        opacity: disabled ? 0.5 : 1,
      }}
    >
      <span>장바구니에 담기</span>
      <span>{price}</span>
    </button>
  )
}
