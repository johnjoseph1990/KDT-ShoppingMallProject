import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getProduct } from '../api/products'
import { addToCart } from '../api/cart'
import { getReviews, createReview, deleteReview } from '../api/reviews'
import { useAuth } from '../context/AuthContext'

function StarRating({ rating }) {
  const filled = Math.round(rating)
  return <span>{Array.from({ length: 5 }, (_, i) => (i < filled ? '★' : '☆')).join('')}</span>
}

export default function ProductDetailPage() {
  const { id } = useParams() // URL에서 :id 추출
  const navigate = useNavigate()
  const { user } = useAuth()

  const [product, setProduct] = useState(null)
  const [reviews, setReviews] = useState([])
  const [quantity, setQuantity] = useState(1)
  const [reviewForm, setReviewForm] = useState({ rating: 5, content: '' })

  useEffect(() => {
    getProduct(id).then((res) => setProduct(res.data))
    loadReviews()
  }, [id])

  const loadReviews = () => {
    getReviews(id).then((res) => setReviews(res.data))
  }

  const handleAddToCart = async () => {
    if (!user) {
      alert('로그인이 필요합니다.')
      navigate('/login')
      return
    }
    try {
      await addToCart({ productId: Number(id), quantity })
      alert('장바구니에 추가되었습니다.')
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

  if (!product) return <p>로딩 중...</p>

  return (
    <div style={{ maxWidth: '720px', margin: '0 auto' }}>
      {/* 상품 정보 */}
      <div style={styles.productSection}>
        {product.imageUrl && (
          <img src={product.imageUrl} alt={product.name} style={styles.productImg} />
        )}
        <div style={styles.productInfo}>
          <h2>{product.name}</h2>
          <p style={styles.price}>{product.price.toLocaleString()}원</p>
          {product.averageRating > 0 && (
            <p style={{ color: '#f4a261' }}>
              <StarRating rating={product.averageRating} /> ({product.averageRating.toFixed(1)})
            </p>
          )}
          <p>{product.description}</p>
          <p style={{ color: product.stockQuantity > 0 ? '#2a9d8f' : '#e63946' }}>
            {product.stockQuantity > 0 ? `재고: ${product.stockQuantity}개` : '품절'}
          </p>
          {product.tags?.length > 0 && (
            <p style={{ color: '#666' }}>태그: {product.tags.join(', ')}</p>
          )}

          {/* 수량 선택 + 장바구니 */}
          <div style={styles.cartRow}>
            <input
              type="number"
              min={1}
              max={product.stockQuantity}
              value={quantity}
              onChange={(e) => setQuantity(Number(e.target.value))}
              style={styles.qtyInput}
              disabled={product.stockQuantity === 0}
            />
            <button
              onClick={handleAddToCart}
              style={styles.cartBtn}
              disabled={product.stockQuantity === 0}
            >
              장바구니 담기
            </button>
          </div>
        </div>
      </div>

      {/* 리뷰 목록 */}
      <section style={styles.reviewSection}>
        <h3>리뷰 ({reviews.length})</h3>
        {reviews.length === 0 ? (
          <p>첫 리뷰를 작성해보세요!</p>
        ) : (
          reviews.map((r) => (
            <div key={r.id} style={styles.reviewCard}>
              <div style={styles.reviewHeader}>
                <strong>{r.memberName}</strong>
                <span style={{ color: '#f4a261' }}>
                  &nbsp;
                  <StarRating rating={r.rating} />
                </span>
                <span style={styles.reviewDate}>{new Date(r.createdAt).toLocaleDateString()}</span>
                {/* 본인 리뷰만 삭제 버튼 표시 */}
                {user && user.id === r.memberId && (
                  <button onClick={() => handleDeleteReview(r.id)} style={styles.deleteBtn}>
                    삭제
                  </button>
                )}
              </div>
              <p style={{ margin: '0.25rem 0 0' }}>{r.content}</p>
            </div>
          ))
        )}

        {/* 리뷰 작성 폼 (로그인한 경우만 표시) */}
        {user && (
          <form onSubmit={handleReviewSubmit} style={styles.reviewForm}>
            <h4>리뷰 작성</h4>
            <label>
              별점:&nbsp;
              <select
                value={reviewForm.rating}
                onChange={(e) => setReviewForm({ ...reviewForm, rating: Number(e.target.value) })}
              >
                {[5, 4, 3, 2, 1].map((v) => (
                  <option key={v} value={v}>
                    {v}점
                  </option>
                ))}
              </select>
            </label>
            <textarea
              value={reviewForm.content}
              onChange={(e) => setReviewForm({ ...reviewForm, content: e.target.value })}
              placeholder="리뷰 내용을 입력하세요"
              rows={3}
              style={styles.textarea}
              required
            />
            <button type="submit" style={styles.cartBtn}>
              리뷰 등록
            </button>
          </form>
        )}
      </section>
    </div>
  )
}

const styles = {
  productSection: { display: 'flex', gap: '2rem', marginBottom: '2rem' },
  productImg: { width: '240px', height: '240px', objectFit: 'cover', borderRadius: '8px' },
  productInfo: { flex: 1 },
  price: { fontSize: '1.4rem', fontWeight: 'bold', color: '#1a1a2e' },
  cartRow: { display: 'flex', gap: '0.75rem', marginTop: '1rem', alignItems: 'center' },
  qtyInput: {
    width: '60px',
    padding: '0.4rem',
    fontSize: '1rem',
    borderRadius: '4px',
    border: '1px solid #ccc',
  },
  cartBtn: {
    padding: '0.6rem 1.2rem',
    background: '#1a1a2e',
    color: '#fff',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
  },
  reviewSection: { borderTop: '1px solid #eee', paddingTop: '1rem' },
  reviewCard: {
    border: '1px solid #eee',
    borderRadius: '6px',
    padding: '0.75rem',
    marginBottom: '0.75rem',
  },
  reviewHeader: { display: 'flex', gap: '0.5rem', alignItems: 'center' },
  reviewDate: { color: '#999', fontSize: '0.8rem', marginLeft: 'auto' },
  deleteBtn: {
    background: 'none',
    border: 'none',
    color: '#e63946',
    cursor: 'pointer',
    fontSize: '0.85rem',
  },
  reviewForm: { display: 'flex', flexDirection: 'column', gap: '0.5rem', marginTop: '1rem' },
  textarea: {
    padding: '0.5rem',
    borderRadius: '4px',
    border: '1px solid #ccc',
    resize: 'vertical',
  },
}
