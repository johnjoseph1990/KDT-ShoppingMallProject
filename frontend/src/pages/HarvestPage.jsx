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

// 기획전에서 강조할 "혜택" 3가지. 할인가(sale price) 필드가 데이터에 없으므로,
// 가짜 할인율을 지어내는 대신 실제로 지킬 수 있는 약속만 문구로 내세운다.
// (데이터에 없는 값을 화면에 박지 않는다 — 재현성/정직성 원칙)
const BENEFITS = [
  { title: '수확 당일 배송', desc: '밭에서 딴 지 하루가 지나지 않은 것만 보냅니다.' },
  { title: '소농 직거래', desc: '대전·충남 스물세 농가에서 곧바로 식탁으로.' },
  { title: '이번 주 한정 수량', desc: '제철에 맞춰 딴 만큼만, 소진되면 다음 주에 다시.' },
]

// HarvestPage: 홈 히어로 "이번 주 수확물 보기" CTA가 도착하는 제철 기획전 페이지.
// "이번주수확" 태그가 붙은 상품을 프로모션 배너 + 혜택 스트립과 함께 보여준다.
export default function HarvestPage() {
  const { showMessage } = useCart()
  const [products, setProducts] = useState([])
  // 로딩 중엔 "불러오는 중…"을, 응답 후엔 결과에 따라 상품 목록 또는 빈 상태를 보여준다.
  // (API 오류 시엔 catch에서 토스트만 띄우고 화면은 빈 상태로 둔다 — 별도 error 상태는 없다)
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
      {/* ─── 프로모션 히어로 배너 ─── */}
      {/* 기존의 밋밋한 텍스트 헤더를 "기획전" 느낌의 진한 배너로 바꿨다.
          이벤트 배지 → 주차 큰 제목 → 수확 기간 순으로 시선을 유도한다. */}
      <section
        style={{
          background: 'var(--color-bg-dark)',
          color: 'var(--color-text-on-dark)',
          padding: 'clamp(56px,9vw,120px) clamp(20px,5vw,72px)',
        }}
      >
        <div style={{ maxWidth: 680, display: 'flex', flexDirection: 'column', gap: 20 }}>
          {/* 이벤트 배지: "지금 진행 중인 기획전"임을 한눈에 알리는 라벨 */}
          <span
            style={{
              alignSelf: 'flex-start',
              border: '1px solid var(--color-text-footer)',
              color: 'var(--color-text-on-dark)',
              fontSize: 12,
              letterSpacing: '0.14em',
              padding: '6px 12px',
            }}
          >
            이번 주 제철 기획전
          </span>
          <h1
            style={{
              margin: 0,
              fontFamily: "'Noto Serif KR', serif",
              fontWeight: 300,
              fontSize: 'clamp(30px,3.6vw,50px)',
              lineHeight: 1.4,
            }}
          >
            {weekLabel},<br />
            밭에서 막 올라온 것들
          </h1>
          <p
            style={{
              margin: 0,
              fontSize: 15,
              lineHeight: 1.9,
              fontWeight: 300,
              color: 'var(--color-text-footer)',
              maxWidth: '46ch',
            }}
          >
            매주 화·금에 수확한 제철 채소·과일만 골라 담았습니다. 이번 주가 지나면 목록이 통째로
            바뀌니, 지금 가장 좋을 때 만나보세요.
          </p>
        </div>
      </section>

      {/* ─── 혜택 스트립 ─── */}
      {/* mobile-1col: 데스크톱 3열 → 모바일에서 1열로 접힘 (index.css) */}
      <section
        className="mobile-1col"
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(3, 1fr)',
          gap: 1,
          background: 'var(--color-border)',
          borderBottom: '1px solid var(--color-border)',
        }}
      >
        {BENEFITS.map((b) => (
          <div
            key={b.title}
            style={{
              background: 'var(--color-bg)',
              padding: 'clamp(24px,3vw,36px) clamp(20px,3vw,40px)',
              display: 'flex',
              flexDirection: 'column',
              gap: 8,
            }}
          >
            <p
              style={{
                margin: 0,
                fontSize: 15,
                fontWeight: 400,
                letterSpacing: '0.02em',
              }}
            >
              {b.title}
            </p>
            <p
              style={{
                margin: 0,
                fontSize: 13,
                lineHeight: 1.7,
                fontWeight: 300,
                color: 'var(--color-fg-muted)',
              }}
            >
              {b.desc}
            </p>
          </div>
        ))}
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
              // 열 수를 상품 개수와 4 중 작은 값으로 잡는다.
              // 4로 고정하면 태그 상품이 1~3개일 때 빈 그리드 칸에 컨테이너 배경
              // (var(--color-border))이 회색 박스처럼 비친다(DEF-4와 동일한 결함).
              // 홈은 최대 4개라 featured.length를 그대로 썼지만, 여기는 size:12라
              // 그대로 쓰면 한 줄에 최대 12열이 되므로 Math.min으로 4열 다행을 유지한다.
              gridTemplateColumns: `repeat(${Math.min(products.length, 4)}, 1fr)`,
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

      {/* ─── 하단 CTA ─── */}
      {/* 기획전은 일부 상품만 보여주므로, "전체 상품 보기"로 자연스럽게 이어준다. */}
      <section
        style={{
          padding: '0 clamp(20px,5vw,72px) clamp(48px,6vw,80px)',
          borderTop: '1px solid var(--color-border)',
          paddingTop: 'clamp(32px,4vw,56px)',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: 16,
        }}
      >
        <TextLink to="/about" style={{ fontSize: 13, color: 'var(--color-fg-muted)' }}>
          ← 브랜드 소개로 돌아가기
        </TextLink>
        <TextLink
          to="/shop"
          style={{
            fontSize: 13,
            letterSpacing: '0.05em',
            borderBottom: '1px solid var(--color-fg)',
            paddingBottom: 2,
          }}
        >
          전체 상품 보기 →
        </TextLink>
      </section>
    </main>
  )
}
