import { useEffect, useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { getProducts } from '../api/products'
import { addToCart } from '../api/cart'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'
import { fmt } from '../utils/product'
// 페이징 경계 판정은 utils/pagination 한 곳에서만 관리한다.
// (예전엔 이 파일에 `(page + 1) * 10 >= totalElements`로 적혀 있었다 — 그 10은
//  백엔드 @PageableDefault(size = 10)의 복사본이라, 서버가 20으로 바꾸면 화면만 조용히 틀려진다)
import { isLastPage } from '../utils/pagination'
import ProductCard from '../components/ProductCard'

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

export default function ProductListPage() {
  const [products, setProducts] = useState([])
  const [totalElements, setTotalElements] = useState(0)
  // totalPages: "다음" 버튼을 잠글지 판정하는 데만 쓴다. 화면 상단의 "N개의 상품"은
  // 여전히 totalElements(전체 건수)를 쓰므로 두 값이 둘 다 필요하다.
  // 초기값 0 — 응답 전에는 isLastPage(0, 0)이 true라 "다음"이 잠긴 상태로 시작한다.
  const [totalPages, setTotalPages] = useState(0)
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
  const { refreshCart, showToast, showMessage } = useCart()

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
        // content 옆에 함께 오는 totalPages를 버리지 않고 챙긴다.
        // ?? 0 은 응답에 값이 없을 때의 대비 — 그 경우 "다음"은 잠긴 상태로 남는다.
        setTotalPages(res.data.totalPages ?? 0)
      })
      .catch(() => showMessage('상품 목록을 불러오지 못했습니다'))
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
      // state: { from: location } — 로그인 페이지에 "로그인 성공하면 여기로 돌아와야 해"라고 알려주는 값
      navigate('/login', { state: { from: location } })
      return
    }
    try {
      await addToCart({ productId: product.id, quantity: 1 })
      refreshCart()
      showToast(product.name)
    } catch (err) {
      showMessage(err.response?.data?.message || '장바구니 추가에 실패했습니다')
    }
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
      <p
        style={{
          margin: '0 0 28px',
          fontSize: 14,
          color: 'var(--color-fg-muted)',
          fontWeight: 300,
        }}
      >
        {totalElements}개의 상품 · 매주 화·금 수확분 기준
      </p>

      {/* 키워드 검색 — form으로 감싸면 입력창에서 Enter만 쳐도 onSubmit이 실행된다.
          flexWrap: 아주 좁은 화면(320px 등)에서 한 줄에 다 못 넣으면 줄을 바꾼다.
          없으면 억지로 한 줄에 밀어넣다가 가로 스크롤이 생긴다. */}
      <form
        onSubmit={handleSearch}
        style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 20 }}
      >
        <input
          type="search"
          value={keywordInput}
          onChange={(e) => setKeywordInput(e.target.value)}
          placeholder="상품명 · 설명으로 검색"
          aria-label="상품 검색"
          style={{
            // flex-basis 180px: 줄바꿈 여부는 "줄어들기 전 기준 크기"로 판정되므로,
            // 기준이 크면(280px) 390px 화면에서 버튼이 아래 줄로 밀려난다.
            // 180으로 낮추면 입력창+검색+초기화가 390px 한 줄에 들어가고,
            // 넓은 화면에서는 grow가 maxWidth 360까지 늘려줘 데스크톱 모습은 그대로다.
            flex: '1 1 180px',
            // minWidth 0: flex 아이템의 기본값 min-width:auto는 "내용보다 작아지지 마라"는
            // 뜻이라 입력창이 안 줄어든다. 0으로 풀어서 줄어드는 몫을 입력창이 떠맡게 한다
            // (버튼 대신 입력창이 좁아지는 게 자연스럽다).
            minWidth: 0,
            maxWidth: 360,
            border: '1px solid var(--color-border)',
            background: 'transparent',
            padding: '10px 14px',
            fontSize: 13,
            fontWeight: 300,
            color: 'var(--color-fg)',
          }}
        />
        {/* flexShrink 0 + whiteSpace nowrap이 짝을 이뤄야 버튼 글자가 안 쪼개진다.
            한글은 글자 사이 어디서나 줄바꿈이 되므로, 폭이 모자라면 "검색"이 "검/색"으로
            세로 분리된다(390px 모바일에서 실제로 발생). nowrap으로 줄바꿈을 막고,
            flexShrink 0으로 애초에 찌그러지지 않게 한다. */}
        <button
          type="submit"
          style={{
            cursor: 'pointer',
            border: '1px solid var(--color-fg)',
            background: 'var(--color-fg)',
            color: 'var(--color-bg)',
            padding: '10px 20px',
            fontSize: 13,
            fontWeight: 300,
            flexShrink: 0,
            whiteSpace: 'nowrap',
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
              border: '1px solid var(--color-border)',
              background: 'transparent',
              color: 'var(--color-fg-muted)',
              padding: '10px 16px',
              fontSize: 13,
              fontWeight: 300,
              // 검색 버튼과 같은 이유 — 이 버튼이 함께 뜨면 폭이 더 빠듯해져
              // "초기화"가 "초기/화"로 쪼개졌다.
              flexShrink: 0,
              whiteSpace: 'nowrap',
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

      {/* 별점 필터 버튼 — 서버의 minRating 파라미터로 걸러지므로 다른 페이지의 상품도 정확히 반영된다.
          role="group" + aria-labelledby: 바로 아래 span('별점')을 이 버튼 묶음의 이름으로 지정한다.
          라벨이 없으면 스크린리더는 위 카테고리 버튼과 구분 없이 10개를 쭉 읽어버린다. */}
      <div
        role="group"
        aria-labelledby="rating-filter-label"
        style={{
          display: 'flex',
          gap: 10,
          marginBottom: 36,
          flexWrap: 'wrap',
          alignItems: 'center', // 라벨 글자와 버튼의 세로 중심을 맞춘다
        }}
      >
        <span
          id="rating-filter-label"
          style={{
            fontSize: 12,
            color: 'var(--color-fg-muted)',
            fontWeight: 300,
            letterSpacing: '0.08em',
            marginRight: 2,
          }}
        >
          별점
        </span>
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
        <p style={{ fontSize: 14, color: 'var(--color-fg-muted)', fontWeight: 300 }}>
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
            background: 'var(--color-border)',
            border: '1px solid var(--color-border)',
          }}
        >
          {products.map((p) => (
            <ProductCard
              key={p.id}
              product={p}
              to={`/products/${p.id}`}
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
        <span style={{ fontSize: 13, color: 'var(--color-fg-muted)', fontWeight: 300 }}>
          페이지 {page + 1}
        </span>
        {/* 백엔드가 계산해서 보내주는 totalPages로 판단한다 — 이 화면은 페이지 크기를 모른다.
            관리자 화면(AdminPage의 Pager)도 같은 함수를 쓰므로 규칙이 어긋나지 않는다 */}
        <PageBtn onClick={() => setPage((p) => p + 1)} disabled={isLastPage(page, totalPages)}>
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
        border: `1px solid ${active ? 'var(--color-fg)' : 'var(--color-border)'}`,
        background: active ? 'var(--color-fg)' : 'transparent',
        color: active ? 'var(--color-bg)' : 'var(--color-fg)',
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

function PageBtn({ onClick, disabled, children }) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      style={{
        cursor: disabled ? 'default' : 'pointer',
        border: '1px solid var(--color-border)',
        background: 'transparent',
        color: 'var(--color-fg)',
        padding: '8px 16px',
        fontSize: 13,
        opacity: disabled ? 0.4 : 1,
      }}
    >
      {children}
    </button>
  )
}
