import { useEffect, useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { getProducts } from '../api/products'
import { addToCart } from '../api/cart'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'

const fmt = (n) => n.toLocaleString('ko-KR') + '원'

/* 카테고리 필터 → 상품 태그(product_tag)로 매핑.
   name/description 부분일치인 keyword 검색이 아니라, 정확히 일치하는
   tag로 필터링해야 "상품명에 우연히 단어가 들어간 것"이 안 섞인다. */
const CATS = [
  { key: 'all', label: '전체', tag: '' },
  { key: 'veg', label: '채소', tag: '채소' },
  { key: 'fruit', label: '과일', tag: '과일' },
  { key: 'meat', label: '정육', tag: '정육' },
  { key: 'bake', label: '베이커리·간식', tag: '베이커리' },
  { key: 'box', label: '꾸러미·정기배송', tag: '꾸러미' },
]

/* 별점 필터 선택지. value가 그대로 백엔드 minRating 파라미터로 나간다.
   null이면 파라미터 자체를 안 붙여서(=필터 미적용) 리뷰 없는 상품까지 모두 조회된다. */
const RATINGS = [
  { key: 'any', label: '전체 별점', value: null },
  { key: 'r3', label: '★ 3.0 이상', value: 3 },
  { key: 'r4', label: '★ 4.0 이상', value: 4 },
  { key: 'r45', label: '★ 4.5 이상', value: 4.5 },
]

/* 재고 수량을 화면 표시용 배지로 바꾼다.
   "몇 개 남았을 때부터 재촉할지"는 정답이 없는 비즈니스 판단이라 이 함수 한 곳에 모아둔다.
   카드의 배지 텍스트/색이 모두 이 결과를 쓰므로, 여기만 고치면 표시 규칙이 일괄로 바뀐다. */
function stockBadge(stockQuantity) {
  // 재고가 남아있으면 배지를 그리지 않는다 (null이면 렌더링 자체를 생략)
  if (stockQuantity > 0) return null
  // 품절일 때만 배지 표시 — 색상은 상세 페이지의 품절 경고색(#e63946)과 통일
  return { text: '품절', color: '#e63946' }
}

export default function ProductListPage() {
  const [products, setProducts] = useState([])
  const [totalElements, setTotalElements] = useState(0)
  const [cat, setCat] = useState('all')
  // keywordInput: 입력창에 타이핑 중인 값 (매 글자마다 바뀜)
  // keyword     : 실제로 서버에 보낸 검색어 (엔터/버튼을 눌러야 바뀜)
  // 두 개로 나누는 이유 — 하나로 합치면 한 글자 칠 때마다 API를 호출하게 된다.
  const [keywordInput, setKeywordInput] = useState('')
  const [keyword, setKeyword] = useState('')
  const [ratingKey, setRatingKey] = useState('any')
  const [page, setPage] = useState(0)
  const navigate = useNavigate()
  // useLocation(): 현재 페이지 경로 정보를 담고 있음. 로그인 후 "원래 있던 페이지"로
  // 돌아오게 하려면, 로그인으로 넘어가기 전에 이 위치 정보를 함께 들고 가야 한다.
  const location = useLocation()
  const { user } = useAuth()
  const { refreshCart, showToast } = useCart()

  useEffect(() => {
    const tag = CATS.find((c) => c.key === cat)?.tag || ''
    const minRating = RATINGS.find((r) => r.key === ratingKey)?.value
    getProducts({
      // 값이 없는 필터는 아예 파라미터를 빼서 보낸다.
      // (빈 문자열 tag=''를 보내면 서버가 "빈 태그로 필터"로 오해할 수 있다)
      ...(tag ? { tag } : {}),
      ...(keyword ? { keyword } : {}),
      ...(minRating != null ? { minRating } : {}),
      page,
    })
      .then((res) => {
        setProducts(res.data.content || [])
        // totalElements: 현재 페이지가 아니라 "필터에 걸린 전체 개수".
        // 페이지당 10개만 받으므로 products.length로는 전체 개수를 알 수 없다.
        setTotalElements(res.data.totalElements ?? 0)
      })
      .catch(() => {})
  }, [cat, keyword, ratingKey, page])

  // 필터를 바꾸면 페이지를 0으로 되돌린다.
  // 안 그러면 3페이지를 보던 중 필터를 걸었을 때 결과가 1페이지뿐인데도 3페이지(=빈 화면)를 요청하게 된다.
  const handleCatChange = (key) => {
    setCat(key)
    setPage(0)
  }

  const handleRatingChange = (key) => {
    setRatingKey(key)
    setPage(0)
  }

  // e.preventDefault(): <form> 기본 동작(페이지 새로고침)을 막는다. SPA에서는 필수.
  const handleSearch = (e) => {
    e.preventDefault()
    setKeyword(keywordInput.trim())
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
      <p style={{ margin: '0 0 28px', fontSize: 14, color: '#6d6c61', fontWeight: 300 }}>
        {totalElements}개의 상품 · 매주 화·금 수확분 기준
      </p>

      {/* 키워드 검색 — form으로 감싸면 입력창에서 Enter만 쳐도 onSubmit이 실행된다 */}
      <form onSubmit={handleSearch} style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
        <input
          type="search"
          value={keywordInput}
          onChange={(e) => setKeywordInput(e.target.value)}
          placeholder="상품명 · 설명으로 검색"
          aria-label="상품 검색"
          style={{
            flex: '1 1 280px',
            maxWidth: 360,
            border: '1px solid #dddaca',
            background: 'transparent',
            padding: '10px 14px',
            fontSize: 13,
            fontWeight: 300,
            color: '#333330',
          }}
        />
        <button
          type="submit"
          style={{
            cursor: 'pointer',
            border: '1px solid #333330',
            background: '#333330',
            color: '#fffef2',
            padding: '10px 20px',
            fontSize: 13,
            fontWeight: 300,
          }}
        >
          검색
        </button>
        {/* 검색어가 적용된 상태에서만 초기화 버튼을 노출한다 */}
        {keyword && (
          <button
            type="button"
            onClick={() => {
              setKeywordInput('')
              setKeyword('')
              setPage(0)
            }}
            style={{
              cursor: 'pointer',
              border: '1px solid #dddaca',
              background: 'transparent',
              color: '#6d6c61',
              padding: '10px 16px',
              fontSize: 13,
              fontWeight: 300,
            }}
          >
            초기화
          </button>
        )}
      </form>

      {/* 카테고리 필터 버튼 */}
      <div style={{ display: 'flex', gap: 10, marginBottom: 12, flexWrap: 'wrap' }}>
        {CATS.map((c) => (
          <CatBtn key={c.key} active={cat === c.key} onClick={() => handleCatChange(c.key)}>
            {c.label}
          </CatBtn>
        ))}
      </div>

      {/* 별점 필터 버튼 — 서버의 minRating 파라미터로 걸러지므로 다른 페이지의 상품도 정확히 반영된다 */}
      <div style={{ display: 'flex', gap: 10, marginBottom: 36, flexWrap: 'wrap' }}>
        {RATINGS.map((r) => (
          <CatBtn
            key={r.key}
            active={ratingKey === r.key}
            onClick={() => handleRatingChange(r.key)}
          >
            {r.label}
          </CatBtn>
        ))}
      </div>

      {/* 3열 상품 그리드 */}
      {products.length === 0 ? (
        <p style={{ fontSize: 14, color: '#6d6c61', fontWeight: 300 }}>
          {keyword || ratingKey !== 'any'
            ? '조건에 맞는 상품이 없습니다. 검색어나 별점 조건을 바꿔보세요.'
            : '상품이 없습니다.'}
        </p>
      ) : (
        // mobile-2col: 데스크톱은 아래 인라인값(3열)을 쓰고,
        // 768px 이하에서는 index.css의 미디어쿼리가 2열로 덮어쓴다
        <div
          className="mobile-2col"
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
        {/* 전체 개수(totalElements) 기준으로 마지막 페이지를 판단한다.
            products.length < 10 으로 판단하면 "정확히 10개"일 때 빈 다음 페이지로 넘어간다 */}
        <PageBtn onClick={() => setPage((p) => p + 1)} disabled={(page + 1) * 10 >= totalElements}>
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
  // 품절 여부는 배지 표시와 무관하게 재고 0으로 직접 판단한다 (버튼 비활성화의 기준)
  const soldOut = product.stockQuantity === 0
  const badge = stockBadge(product.stockQuantity)
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
      {/* 상품 이미지 — position:'relative'를 준 이유:
          안쪽의 배지(position:'absolute')가 "이 박스"를 기준으로 위치를 잡게 하기 위함.
          relative 부모가 없으면 배지가 화면 전체를 기준으로 붙어버린다. */}
      <div
        onClick={onOpen}
        style={{
          cursor: 'pointer',
          aspectRatio: '4/5',
          background: '#edeadb',
          overflow: 'hidden',
          position: 'relative',
        }}
      >
        {product.imageUrl && (
          <img
            src={product.imageUrl}
            alt={product.name}
            style={{
              width: '100%',
              height: '100%',
              objectFit: 'cover',
              // 품절 상품은 흑백 + 반투명으로 눌러 "지금은 못 산다"를 색으로도 전달한다
              filter: soldOut ? 'grayscale(1)' : 'none',
              opacity: soldOut ? 0.5 : 1,
            }}
          />
        )}
        {/* 재고 배지 — stockBadge()가 값을 돌려줄 때만 표시 */}
        {badge && (
          <span
            style={{
              position: 'absolute',
              top: 10,
              left: 10,
              background: badge.color,
              color: '#fffef2',
              fontSize: 11,
              letterSpacing: '0.06em',
              padding: '4px 9px',
              fontWeight: 400,
            }}
          >
            {badge.text}
          </span>
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
        {/* 평균 별점 — 리뷰가 없으면 서버가 0.0을 내려주므로 그때는 '리뷰 없음'으로 표시한다.
            toFixed(1): 4.3333... 같은 값을 소수점 한 자리로 반올림해 문자열로 만든다. */}
        <p style={{ margin: '2px 0 0', fontSize: 12, color: '#6d6c61', fontWeight: 300 }}>
          {product.averageRating > 0 ? `★ ${product.averageRating.toFixed(1)}` : '리뷰 없음'}
        </p>
        <p style={{ margin: '6px 0 0', fontSize: 14 }}>{fmt(product.price)}</p>
      </div>

      {/* 장바구니 버튼 — 텍스트 대신 카트 아이콘만 표시.
          텍스트가 없으므로 스크린리더용으로 aria-label을 반드시 붙인다.
          SVG는 stroke="currentColor"라 버튼 color를 따라가고, hover 시 함께 반전된다. */}
      {/* disabled: 품절이면 클릭 자체를 막는다. onClick 안에서 걸러도 되지만,
          disabled를 쓰면 키보드 탭 이동에서도 건너뛰어져 접근성 측면에서 더 정확하다. */}
      <button
        onClick={onAdd}
        disabled={soldOut}
        aria-label={soldOut ? '품절된 상품' : '장바구니에 담기'}
        style={{
          cursor: soldOut ? 'not-allowed' : 'pointer',
          border: `1px solid ${soldOut ? '#dddaca' : '#333330'}`,
          background: 'transparent',
          color: soldOut ? '#a8a495' : '#333330',
          padding: '12px',
          // 아이콘을 버튼 가운데로 정렬
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
        onMouseEnter={(e) => {
          if (soldOut) return // 품절 버튼은 hover 반전을 하지 않는다
          e.currentTarget.style.background = '#333330'
          e.currentTarget.style.color = '#fffef2'
        }}
        onMouseLeave={(e) => {
          if (soldOut) return
          e.currentTarget.style.background = 'transparent'
          e.currentTarget.style.color = '#333330'
        }}
      >
        {/* 카트 아이콘: 바퀴 두 개(circle) + 카트 몸통(path) */}
        <svg
          width="20"
          height="20"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.5"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <circle cx="9" cy="20" r="1" />
          <circle cx="18" cy="20" r="1" />
          <path d="M2 3h3l2.4 12.4a1 1 0 0 0 1 .8h9.2a1 1 0 0 0 1-.8L21 7H6" />
        </svg>
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
