import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
// Vapor UI 컴포넌트: Button(버튼), TextInput(입력창). 인라인 style 대신 props(size/colorPalette/variant)로 모양을 정함
import { Button, TextInput } from '@vapor-ui/core'
import { getProducts, getBestProducts } from '../api/products'

// ★ 별점을 숫자로 받아 별 문자로 변환
function StarRating({ rating }) {
  const filled = Math.round(rating)
  return <span>{Array.from({ length: 5 }, (_, i) => (i < filled ? '★' : '☆')).join('')}</span>
}

export default function ProductListPage() {
  const [products, setProducts] = useState([])
  const [bestProducts, setBestProducts] = useState([])
  const [keyword, setKeyword] = useState('')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)

  // 베스트 상품(별점 기준 상위 5개)은 한 번만 로드
  // 백엔드가 Page 객체로 응답하므로 실제 배열은 res.data.content에 들어있다
  useEffect(() => {
    getBestProducts()
      .then((res) => setBestProducts(res.data.content))
      .catch(() => {})
  }, [])

  // 검색어 또는 페이지가 바뀔 때마다 상품 목록 다시 로드
  useEffect(() => {
    getProducts({ ...(search ? { keyword: search } : {}), page })
      .then((res) => setProducts(res.data.content))
      .catch(() => {})
  }, [search, page])

  const handleSearch = (e) => {
    e.preventDefault()
    setPage(0) // 새 검색은 항상 1페이지부터
    setSearch(keyword)
  }

  return (
    <div>
      {/* 베스트 상품 섹션 */}
      {bestProducts.length > 0 && (
        <section style={styles.section}>
          <h2>🏆 베스트 상품</h2>
          <div style={styles.grid}>
            {bestProducts.map((p) => (
              <ProductCard key={p.id} product={p} />
            ))}
          </div>
        </section>
      )}

      {/* 전체 상품 + 검색 */}
      <section style={styles.section}>
        <h2>전체 상품</h2>
        <form onSubmit={handleSearch} style={styles.searchForm}>
          {/* onChange 대신 onValueChange: Vapor TextInput은 문자열 값을 바로 넘겨줌 */}
          <TextInput
            value={keyword}
            onValueChange={setKeyword}
            placeholder="상품명 검색"
            style={{ flex: 1 }}
          />
          <Button type="submit" colorPalette="primary">
            검색
          </Button>
        </form>
        {products.length === 0 ? (
          <p>상품이 없습니다.</p>
        ) : (
          <div style={styles.grid}>
            {products.map((p) => (
              <ProductCard key={p.id} product={p} />
            ))}
          </div>
        )}
        {/* 페이지네이션 */}
        <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem', alignItems: 'center' }}>
          <Button
            variant="outline"
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
          >
            이전
          </Button>
          <span>페이지 {page + 1}</span>
          <Button
            variant="outline"
            onClick={() => setPage((p) => p + 1)}
            disabled={products.length === 0}
          >
            다음
          </Button>
        </div>
      </section>
    </div>
  )
}

// 상품 카드 컴포넌트: 목록에서 반복 사용
function ProductCard({ product }) {
  return (
    <Link to={`/products/${product.id}`} style={styles.cardLink}>
      <div style={styles.card}>
        {product.imageUrl && (
          <img src={product.imageUrl} alt={product.name} style={styles.cardImg} />
        )}
        <h3 style={styles.cardTitle}>{product.name}</h3>
        <p style={styles.cardPrice}>{product.price.toLocaleString()}원</p>
        {product.averageRating > 0 && (
          <p style={styles.cardRating}>
            <StarRating rating={product.averageRating} /> ({product.averageRating.toFixed(1)})
          </p>
        )}
        <p style={{ color: product.stockQuantity > 0 ? '#2a9d8f' : '#e63946', margin: 0 }}>
          {product.stockQuantity > 0 ? `재고 ${product.stockQuantity}개` : '품절'}
        </p>
      </div>
    </Link>
  )
}

const styles = {
  section: { marginBottom: '2rem' },
  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))',
    gap: '1rem',
  },
  cardLink: { textDecoration: 'none', color: 'inherit' },
  card: {
    border: '1px solid #ddd',
    borderRadius: '8px',
    padding: '1rem',
    transition: 'box-shadow 0.2s',
    cursor: 'pointer',
  },
  cardImg: { width: '100%', height: '140px', objectFit: 'cover', borderRadius: '4px' },
  cardTitle: { margin: '0.5rem 0 0.25rem', fontSize: '0.95rem' },
  cardPrice: { margin: '0 0 0.25rem', fontWeight: 'bold', color: '#1a1a2e' },
  cardRating: { margin: '0 0 0.25rem', color: '#f4a261', fontSize: '0.85rem' },
  searchForm: { display: 'flex', gap: '0.5rem', marginBottom: '1rem' },
}
