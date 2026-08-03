import { useEffect, useState } from 'react'
import { useParams, useNavigate, useLocation } from 'react-router-dom'
import { getProduct, getRecommendations, getKeywords } from '../api/products'
import { addToCart } from '../api/cart'
import { getReviews, createReview, updateReview, deleteReview } from '../api/reviews'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'
import { fmt } from '../utils/product'
import TextLink from '../components/TextLink'
// 추천 상품 카드는 목록·홈과 같은 컴포넌트를 재사용한다 (DEF-12에서 복제본을 걷어냈다)
import ProductCard from '../components/ProductCard'

export default function ProductDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const { user } = useAuth()
  const { refreshCart, showToast, showMessage } = useCart()

  const [product, setProduct] = useState(null)
  // 상품 조회 실패 시 true로 전환 — !product와 구분해 "로딩 중"과 "실패"를 분리한다
  const [loadError, setLoadError] = useState(false)
  const [reviews, setReviews] = useState([])
  // 같은 태그를 가진 다른 상품 추천 목록 (R-6)
  const [recommendations, setRecommendations] = useState([])
  // 이 상품의 리뷰에서 추출된 키워드 목록 (count 내림차순으로 서버가 정렬해서 줌)
  const [keywords, setKeywords] = useState([])
  const [quantity, setQuantity] = useState(1)
  const [reviewForm, setReviewForm] = useState({ rating: 5, content: '' })
  // 리뷰 수정 상태 — null이면 아무것도 편집 중이 아니고,
  // { id, rating, content } 형태면 그 id의 리뷰가 인라인 편집 중이라는 뜻이다.
  // 상태 하나로 "편집 여부 + 대상 + 입력값"을 모두 표현하므로 여러 폼이 동시에 열릴 수 없다.
  const [editing, setEditing] = useState(null)
  // [P1-5] Page 응답의 totalElements로 실제 리뷰 총 수를 보여준다.
  const [reviewTotal, setReviewTotal] = useState(0)

  useEffect(() => {
    // ignore 플래그: id가 바뀌어 이전 요청의 응답이 늦게 도착해도 상태를 덮어쓰지 않게 한다.
    // (사용자가 상품 A → B → C 빠르게 이동할 때 A 응답이 C 화면을 덮는 race condition 방지)
    // id가 바뀌면(추천 상품 클릭 등) 항상 맨 위부터 보여준다
    window.scrollTo({ top: 0, behavior: 'instant' })

    let ignore = false

    setProduct(null)
    setLoadError(false)

    getProduct(id)
      .then((res) => {
        if (!ignore) setProduct(res.data)
      })
      .catch(() => {
        if (!ignore) setLoadError(true)
      })

    // 추천 상품은 부가 데이터 — 실패해도 화면이 깨지지 않으므로 조용히 무시
    getRecommendations(id)
      .then((res) => {
        if (!ignore) setRecommendations(res.data)
      })
      .catch(() => {})

    loadKeywords()
    loadReviews()

    // 클린업 함수: 이 effect가 재실행되거나 컴포넌트가 언마운트될 때 실행
    return () => {
      ignore = true
    }
  }, [id])

  const loadReviews = () => {
    // [P1-5] Page 응답: content(목록)와 totalElements(전체 수)를 분리해서 사용한다.
    getReviews(id, { page: 0, size: 20 })
      .then((res) => {
        setReviews(res.data.content)
        setReviewTotal(res.data.totalElements)
      })
      .catch(() => {})
  }

  // 리뷰 키워드 조회 (리뷰가 없으면 빈 배열이 돌아옴). 리뷰가 작성·수정·삭제될 때마다
  // 서버가 재추출한 키워드로 다시 맞춰야 하므로 loadReviews와 항상 같이 호출한다.
  const loadKeywords = () => {
    getKeywords(id)
      .then((res) => setKeywords(res.data))
      .catch(() => {})
  }

  const handleAddToCart = async () => {
    if (!user) {
      navigate('/login', { state: { from: location } })
      return
    }
    try {
      await addToCart({ productId: Number(id), quantity })
      refreshCart()
      showToast(product.name)
    } catch (err) {
      showMessage(err.response?.data?.message || '장바구니 추가에 실패했습니다')
    }
  }

  // 장바구니를 거치지 않고 바로 주문하기 페이지로 이동한다.
  // 내부적으로는 "장바구니 담기 + /cart 이동"으로 구현 — 별도 API 없이 기존 흐름 재사용.
  const handleBuyNow = async () => {
    if (!user) {
      navigate('/login', { state: { from: location } })
      return
    }
    try {
      await addToCart({ productId: Number(id), quantity })
      refreshCart()
      navigate('/cart')
    } catch (err) {
      showMessage(err.response?.data?.message || '구매 처리에 실패했습니다')
    }
  }

  const handleReviewSubmit = async (e) => {
    e.preventDefault()
    try {
      await createReview(id, reviewForm)
      setReviewForm({ rating: 5, content: '' })
      loadReviews()
      loadKeywords()
    } catch (err) {
      showMessage(err.response?.data?.message || '리뷰 작성에 실패했습니다')
    }
  }

  // 수정 시작: 서버에서 받은 리뷰 값을 editing으로 "복사"해 온다.
  // reviews 배열을 직접 건드리지 않기 때문에 취소하면 원본이 그대로 남는다.
  const startEdit = (r) => setEditing({ id: r.id, rating: r.rating, content: r.content })

  // 수정 취소: 편집 상태를 비우면 화면은 원래 리뷰 본문으로 되돌아간다.
  const cancelEdit = () => setEditing(null)

  const handleUpdateReview = async (e) => {
    // form의 기본 동작(페이지 새로고침)을 막는다. SPA에서는 필수.
    e.preventDefault()
    try {
      // editing에는 서버가 쓰지 않는 id도 들어있으므로 rating·content만 골라 보낸다.
      await updateReview(id, editing.id, { rating: editing.rating, content: editing.content })
      // 성공했을 때만 편집 폼을 닫고, 목록을 서버에서 다시 읽어온다.
      // (작성·삭제와 같은 방식 — 화면에 보이는 값의 출처를 항상 서버로 통일한다)
      cancelEdit()
      loadReviews()
      loadKeywords()
    } catch (err) {
      // 실패 시에는 편집 폼을 열어둔다. 닫아버리면 방금 입력한 내용이 사라진다.
      showMessage(err.response?.data?.message || '리뷰 수정에 실패했습니다')
    }
  }

  const handleDeleteReview = async (reviewId) => {
    if (!confirm('리뷰를 삭제하시겠습니까?')) return
    try {
      await deleteReview(id, reviewId)
      loadReviews()
      loadKeywords()
    } catch (err) {
      showMessage(err.response?.data?.message || '삭제 권한이 없습니다')
    }
  }

  // 에러와 로딩을 구분해 렌더링한다.
  // 기존에 !product 하나로 둘 다 처리하면 실패해도 영원히 "불러오는 중..."이 표시된다.
  if (loadError)
    return (
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          flexDirection: 'column',
          gap: 16,
          minHeight: '60vh',
          fontSize: 14,
          color: 'var(--color-fg-muted)',
          fontWeight: 300,
        }}
      >
        <p style={{ margin: 0 }}>상품 정보를 불러오지 못했습니다.</p>
        {/* 다른 곳으로 가는 게 아니라 현재 페이지를 다시 불러오는 "동작"이라 <button> */}
        <button
          type="button"
          onClick={() => window.location.reload()}
          style={{
            cursor: 'pointer',
            fontSize: 13,
            background: 'none',
            border: 'none',
            borderBottom: '1px solid var(--color-fg-muted)',
            padding: 0,
            fontFamily: 'inherit',
            color: 'inherit',
          }}
        >
          새로고침
        </button>
      </div>
    )

  if (!product)
    return (
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: '60vh',
          fontSize: 14,
          color: 'var(--color-fg-muted)',
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
          borderBottom: '1px solid var(--color-border)',
          minHeight: '76vh',
        }}
      >
        {/* 왼쪽: 스크롤 시 뷰포트에 고정되는 상품 이미지.
            hero-sticky-image: 모바일에서는 고정 해제 + 높이 축소 (화면 독차지 방지) */}
        <div
          className="hero-sticky-image"
          style={{
            background: 'var(--color-bg-hover)',
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
            <div style={{ width: '100%', height: '100%', background: 'var(--color-bg-hover)' }} />
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
          {/* 이동이므로 진짜 링크로 (DEF-7) */}
          <TextLink to="/shop" style={{ fontSize: 13, color: 'var(--color-fg-muted)' }}>
            ← 쇼핑으로 돌아가기
          </TextLink>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {product.tags?.length > 0 && (
              <p
                style={{
                  margin: 0,
                  fontSize: 12,
                  letterSpacing: '0.14em',
                  color: 'var(--color-fg-accent)',
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
                color: 'var(--color-fg-muted)',
                fontWeight: 300,
                lineHeight: 1.9,
              }}
            >
              {product.description}
            </p>
          </div>

          {/* 조회수 — API 응답의 viewCount를 그대로 표시 */}
          <p style={{ margin: 0, fontSize: 13, color: 'var(--color-fg-muted)' }}>
            조회 {product.viewCount.toLocaleString()}회
          </p>

          {/* 재고 상태 */}
          <p
            style={{
              margin: 0,
              fontSize: 14,
              color: product.stockQuantity > 0 ? 'var(--color-fg-accent)' : 'var(--color-danger)',
            }}
          >
            {product.stockQuantity > 0 ? `재고 ${product.stockQuantity}개` : '품절'}
          </p>

          {/* 총액 요약: 수량 × 단가 — 수량 스테퍼와 연동해 실시간 반영 */}
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'baseline',
              padding: '16px 0',
              borderTop: '1px solid var(--color-border)',
              borderBottom: '1px solid var(--color-border)',
            }}
          >
            <span style={{ fontSize: 12, letterSpacing: '0.1em', color: 'var(--color-fg-muted)' }}>
              총 금액
            </span>
            <span style={{ fontSize: 22, fontWeight: 400, letterSpacing: '-0.01em' }}>
              {fmt(product.price * quantity)}
            </span>
          </div>

          {/* 수량 스테퍼 + 버튼 영역: 세로로 쌓아 바로 구매를 아래 행에 배치 */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {/* 첫 행: 수량 스테퍼 + 장바구니 담기 */}
            <div style={{ display: 'flex', gap: 14, alignItems: 'stretch' }}>
              <div style={{ display: 'flex', border: '1px solid var(--color-fg)' }}>
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
                  onClick={() =>
                    setQuantity((q) => Math.min(product.stockQuantity || q + 1, q + 1))
                  }
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
              <AddCartBtn onClick={handleAddToCart} disabled={product.stockQuantity === 0} />
            </div>
            {/* 두 번째 행: 바로 구매 — 장바구니 담기 + 주문하기 페이지로 즉시 이동 */}
            <BuyNowBtn onClick={handleBuyNow} disabled={product.stockQuantity === 0} />
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
          {/* 빈 칸에 회색 배경이 비치는 문제(홈 베스트 섹션의 DEF-4와 같은 원인).
              이 그리드는 컨테이너 배경(테두리색) + gap 1px로 카드 사이 경계선을 그린다.
              그래서 카드가 덮지 않은 영역은 그대로 회색으로 드러난다 —
              추천이 2개면 4열 중 2칸이 회색 박스가 됐다.

              열 수만 카드 수에 맞추면 회색은 사라지지만 카드가 절반 폭으로 커져
              부가 정보인 추천 섹션이 화면을 압도한다. 그래서 열 수와 함께
              컨테이너 폭도 개수에 비례시켜(1개당 25%) 카드 폭을 4열 기준으로 유지한다.
              → 추천이 2개든 4개든 카드 모양이 같고, 남는 공간에는 컨테이너 자체가 없다.
              모바일에서는 2열로 접히므로 이 폭 제한을 풀어야 한다(index.css의 .rec-grid). */}
          <div
            className="mobile-2col rec-grid"
            style={{
              display: 'grid',
              gridTemplateColumns: `repeat(${Math.min(recommendations.length, 4)},1fr)`,
              maxWidth: `${Math.min(recommendations.length, 4) * 25}%`,
              gap: 1,
              background: 'var(--color-border)',
              border: '1px solid var(--color-border)',
            }}
          >
            {/* 여기는 원래 <div onClick={() => navigate(...)}>로 카드를 직접 그려놨다.
                div는 브라우저에게 그냥 상자라서 Tab으로 도달할 수 없고 Enter도 안 먹으며
                "새 탭에서 열기"도 불가능했다 — 마우스 사용자 전용 UI였다(2026-08-02 DEF-12).

                DEF-7에서 ProductCard를 stretched link로 고쳤는데도 이 카드들만 남은 이유가
                바로 "복제본이었기 때문"이다. 컴포넌트를 고쳐도 컴포넌트를 안 쓰는 곳은
                안 고쳐진다. 그래서 수정 = ProductCard로 교체 = 중복 제거다.

                featured를 쓰는 이유: 홈 베스트 섹션도 똑같은 4열 그리드에 이 모드를 쓴다
                (배지·별점·장바구니 버튼 없는 축약형). 축약 카드의 정본이 이미 있으므로
                새 변종을 만들지 않고 그것을 재사용한다. */}
            {recommendations.map((r) => (
              <ProductCard key={r.id} product={r} to={`/products/${r.id}`} featured />
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
          리뷰 ({reviewTotal})
        </h2>

        {/* 리뷰 본문에서 추출된 키워드 배지. 리뷰가 없으면 keywords도 빈 배열이라 자동으로 숨겨짐 */}
        {keywords.length > 0 && (
          <p style={{ margin: '-16px 0 32px', fontSize: 13, color: 'var(--color-fg-accent)' }}>
            자주 언급된 키워드 · {keywords.map((k) => `${k.keyword}(${k.count})`).join(' · ')}
          </p>
        )}

        {reviews.length === 0 ? (
          <p style={{ fontSize: 14, color: 'var(--color-fg-muted)', fontWeight: 300 }}>
            첫 리뷰를 작성해보세요.
          </p>
        ) : (
          <div
            style={{
              display: 'flex',
              flexDirection: 'column',
              borderTop: '1px solid var(--color-border)',
            }}
          >
            {reviews.map((r) => (
              <div
                key={r.id}
                style={{ padding: '24px 0', borderBottom: '1px solid var(--color-border)' }}
              >
                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    marginBottom: 8,
                  }}
                >
                  <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
                    <strong style={{ fontSize: 14 }}>{r.memberName}</strong>
                    <span style={{ fontSize: 13, color: 'var(--color-fg-accent)' }}>
                      {'★'.repeat(r.rating)}
                      {'☆'.repeat(5 - r.rating)}
                    </span>
                    <span style={{ fontSize: 12, color: 'var(--color-fg-muted)', fontWeight: 300 }}>
                      {new Date(r.createdAt).toLocaleDateString()}
                    </span>
                  </div>
                  {/* 본인 리뷰에만 수정·삭제 버튼을 보여준다.
                      (실제 권한 검증은 백엔드 ReviewService에서 하므로 이건 UX용 숨김일 뿐이다) */}
                  {user && user.id === r.memberId && editing?.id !== r.id && (
                    <div style={{ display: 'flex', gap: 12 }}>
                      <button
                        onClick={() => startEdit(r)}
                        style={{
                          cursor: 'pointer',
                          border: 'none',
                          background: 'transparent',
                          fontSize: 12,
                          color: 'var(--color-fg-muted)',
                        }}
                      >
                        수정
                      </button>
                      <button
                        onClick={() => handleDeleteReview(r.id)}
                        style={{
                          cursor: 'pointer',
                          border: 'none',
                          background: 'transparent',
                          fontSize: 12,
                          color: 'var(--color-fg-muted)',
                        }}
                      >
                        삭제
                      </button>
                    </div>
                  )}
                </div>
                {/* 편집 중인 리뷰는 본문 대신 수정 폼을 렌더링한다 (인라인 편집) */}
                {editing?.id === r.id ? (
                  <form
                    onSubmit={handleUpdateReview}
                    style={{ display: 'flex', flexDirection: 'column', gap: 10, maxWidth: 480 }}
                  >
                    <select
                      value={editing.rating}
                      // 기존 editing 객체를 펼치고(...) rating만 새 값으로 덮어쓴다.
                      // 객체를 새로 만들어야 React가 상태 변경을 감지해 다시 그린다.
                      onChange={(e) => setEditing({ ...editing, rating: Number(e.target.value) })}
                      style={{
                        border: '1px solid var(--color-border)',
                        background: 'transparent',
                        padding: '8px 12px',
                        fontSize: 14,
                        outline: 'none',
                        alignSelf: 'flex-start',
                      }}
                    >
                      {[5, 4, 3, 2, 1].map((v) => (
                        <option key={v} value={v}>
                          {v}점
                        </option>
                      ))}
                    </select>
                    <textarea
                      value={editing.content}
                      onChange={(e) => setEditing({ ...editing, content: e.target.value })}
                      rows={3}
                      required
                      style={{
                        border: '1px solid var(--color-border)',
                        background: 'transparent',
                        padding: 12,
                        fontSize: 14,
                        outline: 'none',
                        resize: 'vertical',
                        fontWeight: 300,
                      }}
                    />
                    <div style={{ display: 'flex', gap: 10 }}>
                      <button
                        type="submit"
                        style={{
                          cursor: 'pointer',
                          border: '1px solid var(--color-fg)',
                          background: 'var(--color-fg)',
                          color: 'var(--color-bg)',
                          padding: '10px 18px',
                          fontSize: 13,
                        }}
                      >
                        저장
                      </button>
                      {/* type="button": 기본값 submit이라 명시하지 않으면 취소 버튼이 폼을 제출해버린다 */}
                      <button
                        type="button"
                        onClick={cancelEdit}
                        style={{
                          cursor: 'pointer',
                          border: '1px solid var(--color-border)',
                          background: 'transparent',
                          padding: '10px 18px',
                          fontSize: 13,
                          color: 'var(--color-fg-muted)',
                        }}
                      >
                        취소
                      </button>
                    </div>
                  </form>
                ) : (
                  <p
                    style={{
                      margin: 0,
                      fontSize: 14,
                      lineHeight: 1.8,
                      fontWeight: 300,
                      color: 'var(--color-fg-subtle)',
                    }}
                  >
                    {r.content}
                  </p>
                )}
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
                color: 'var(--color-fg-muted)',
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
                  border: '1px solid var(--color-border)',
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
                color: 'var(--color-fg-muted)',
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
                  border: '1px solid var(--color-border)',
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
                border: '1px solid var(--color-fg)',
                background: 'var(--color-fg)',
                color: 'var(--color-bg)',
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

// 바로 구매 버튼 — outline 스타일로 장바구니 담기(filled)와 시각적 위계를 구분한다.
function BuyNowBtn({ onClick, disabled }) {
  const [hovered, setHovered] = useState(false)
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      onMouseEnter={() => !disabled && setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        cursor: disabled ? 'default' : 'pointer',
        width: '100%',
        border: '1px solid var(--color-fg)',
        background: hovered ? 'var(--color-fg)' : 'transparent',
        color: hovered ? 'var(--color-bg)' : 'var(--color-fg)',
        padding: '16px 22px',
        fontSize: 14,
        letterSpacing: '0.04em',
        opacity: disabled ? 0.5 : 1,
        transition: 'background 0.15s, color 0.15s',
      }}
    >
      바로 구매
    </button>
  )
}

function AddCartBtn({ onClick, disabled }) {
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
        border: `1px solid ${hovered ? 'var(--color-fg-accent)' : 'var(--color-fg)'}`,
        background: hovered ? 'var(--color-fg-accent)' : 'var(--color-fg)',
        color: 'var(--color-bg)',
        padding: '16px 22px',
        fontSize: 14,
        letterSpacing: '0.04em',
        opacity: disabled ? 0.5 : 1,
      }}
    >
      장바구니에 담기
    </button>
  )
}
