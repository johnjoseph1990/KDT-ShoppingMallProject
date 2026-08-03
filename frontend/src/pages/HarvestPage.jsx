import { useEffect, useState } from 'react'
import { getProducts } from '../api/products'
import { useCart } from '../context/CartContext'
import ProductCard from '../components/ProductCard'
import TextLink from '../components/TextLink'

// 현재 날짜로부터 "N월 N째 주" 형식의 주차 레이블을 계산한다.
// 예: 8월 3일 → "8월 첫째 주"
function getWeekLabel() {
  const now = new Date()
  const month = now.getMonth() + 1
  const week = Math.ceil(now.getDate() / 7)
  const labels = ['첫째', '둘째', '셋째', '넷째', '다섯째']
  return `${month}월 ${labels[week - 1] ?? '마지막'} 주`
}

export default function HarvestPage() {
  const { showMessage } = useCart()
  const [products, setProducts] = useState([])
  // 로딩·에러 상태를 구분해 "불러오는 중"과 "비었음"을 따로 처리한다.
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    // "이번주수확" 태그가 붙은 상품만 가져온다. size:12는 최대 12개까지만 요청.
    getProducts({ tag: '이번주수확', size: 12 })
      .then((res) => setProducts(res.data.content || []))
      .catch(() => showMessage('수확물 목록을 불러오지 못했습니다'))
      .finally(() => setLoading(false))
  }, [])

  const weekLabel = getWeekLabel()

  return (
    <main style={{ animation: 'fadeUp .4s ease both', flex: 1 }}>
      {/* ─── 헤로 섹션 ─── */}
      <section
        style={{
          padding: 'clamp(56px,9vw,120px) clamp(20px,5vw,72px)',
          borderBottom: '1px solid var(--color-border)',
        }}
      >
        <p
          style={{
            margin: '0 0 16px',
            fontSize: 12,
            letterSpacing: '0.14em',
            color: 'var(--color-fg-accent)',
          }}
        >
          이번 주 수확물
        </p>
        <h1
          style={{
            margin: 0,
            fontFamily: "'Noto Serif KR', serif",
            fontWeight: 300,
            fontSize: 'clamp(28px,3.2vw,44px)',
            lineHeight: 1.5,
          }}
        >
          {weekLabel},<br />밭에서 온 것들
        </h1>
      </section>

      {/* ─── 상품 그리드 ─── */}
      <section style={{ padding: 'clamp(40px,5vw,72px) clamp(20px,5vw,72px)' }}>
        {loading ? (
          <p style={{ fontSize: 14, color: 'var(--color-fg-muted)', fontWeight: 300 }}>
            불러오는 중...
          </p>
        ) : products.length === 0 ? (
          // 수확물이 없을 때: 빈 상태 메시지
          <div
            style={{
              display: 'flex',
              flexDirection: 'column',
              gap: 12,
              padding: 'clamp(48px,6vw,80px) 0',
            }}
          >
            <p style={{ margin: 0, fontSize: 16, fontWeight: 300 }}>
              이번 주 수확물을 준비 중입니다.
            </p>
            <p style={{ margin: 0, fontSize: 14, color: 'var(--color-fg-muted)', fontWeight: 300 }}>
              다음 주를 기대해주세요.
            </p>
          </div>
        ) : (
          // mobile-2col: 768px 이하에서 2열로 접힘 (index.css)
          <div
            className="mobile-2col"
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(4, 1fr)',
              gap: 1,
              background: 'var(--color-border)',
              border: '1px solid var(--color-border)',
            }}
          >
            {products.map((p) => (
              <ProductCard key={p.id} product={p} to={`/products/${p.id}`} />
            ))}
          </div>
        )}
      </section>

      {/* ─── 하단 링크 ─── */}
      <section
        style={{
          padding: '0 clamp(20px,5vw,72px) clamp(48px,6vw,80px)',
          borderTop: '1px solid var(--color-border)',
          paddingTop: 'clamp(32px,4vw,56px)',
        }}
      >
        <TextLink to="/about" style={{ fontSize: 13, color: 'var(--color-fg-muted)' }}>
          ← 브랜드 소개로 돌아가기
        </TextLink>
      </section>
    </main>
  )
}