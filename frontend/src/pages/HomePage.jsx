import { useEffect, useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { getBestProducts } from '../api/products'
import { addToCart } from '../api/cart'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'
import ProductCard from '../components/ProductCard'

/* 백엔드 /api/products/best 의 source 값(REVIEW_BEST/SALES/LATEST)을
   화면 문구로 바꿔주는 매핑. 콜드스타트 폴백 단계마다 사용자에게
   "이게 왜 베스트인지"를 정직하게 알려주기 위함 (P0-1 대응) */
const BEST_SOURCE_LABEL = {
  REVIEW_BEST: '리뷰 평점 기준 베스트',
  SALES: '판매량 기준 인기 상품',
  LATEST: '새로 들어온 상품',
}

export default function HomePage() {
  const [featured, setFeatured] = useState([])
  // 이번 응답이 3단계 폴백 중 어느 단계에서 나왔는지 (뱃지 문구 결정용)
  const [bestSource, setBestSource] = useState(null)
  const navigate = useNavigate()
  const location = useLocation()
  const { user } = useAuth()
  const { refreshCart, showToast, showMessage } = useCart()

  /* 베스트 상품 상위 4개를 홈 히어로 그리드에 표시.
     같은 페이지 안의 상품들은 항상 같은 폴백 단계에서 나오므로
     (ProductService.findBestProducts가 단계별로 배타적으로 조회),
     첫 번째 상품의 source만 대표값으로 써도 된다. */
  useEffect(() => {
    getBestProducts()
      .then((res) => {
        const content = res.data.content || []
        setFeatured(content.slice(0, 4))
        setBestSource(content[0]?.source ?? null)
      })
      .catch(() => showMessage('상품을 불러오지 못했습니다'))
  }, [])

  const handleAddToCart = async (product) => {
    if (!user) {
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
    <div style={{ animation: 'fadeUp .5s ease both', flex: 1 }}>
      {/* ─── 히어로 섹션 ─── */}
      <section
        style={{
          position: 'relative',
          minHeight: '88vh',
          borderBottom: '1px solid #dddaca',
          overflow: 'hidden',
          background: '#2a2a20',
        }}
      >
        <img
          src="/uploads/hero-local-vegetables.jpg"
          alt=""
          style={{
            position: 'absolute',
            inset: 0,
            width: '100%',
            height: '100%',
            objectFit: 'cover',
            objectPosition: '70% 50%',
          }}
        />
        <div
          style={{
            position: 'absolute',
            inset: 0,
            background:
              'linear-gradient(90deg,rgba(30,30,22,0.6) 0%,rgba(30,30,22,0.15) 55%,transparent 100%),' +
              'linear-gradient(180deg,rgba(30,30,22,0.15),rgba(30,30,22,0.7))',
          }}
        />
        <div
          style={{
            position: 'relative',
            minHeight: '88vh',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'center',
            padding: 'clamp(32px,6vw,96px)',
            gap: 28,
            maxWidth: 640,
          }}
        >
          <p
            style={{
              margin: 0,
              fontSize: 13,
              letterSpacing: '0.14em',
              color: '#d9d6bc',
              fontWeight: 400,
            }}
          >
            제철 · 대전충남 · 소농직거래
          </p>
          <h1
            style={{
              margin: 0,
              fontFamily: "'Noto Serif KR', serif",
              fontWeight: 300,
              color: '#fffef2',
              fontSize: 'clamp(30px,3.8vw,50px)',
              lineHeight: 1.45,
              letterSpacing: '-0.01em',
              textShadow: '0 1px 8px rgba(0,0,0,0.4)',
            }}
          >
            땅이 허락한 만큼만,
            <br />
            계절이 내어준 순서대로.
          </h1>
          <p
            style={{
              margin: 0,
              fontSize: 15,
              lineHeight: 1.9,
              color: '#e5e3d3',
              fontWeight: 300,
              maxWidth: '44ch',
            }}
          >
            민스 파머스 마켓은 대전과 충남의 스물세 곳 소농과 함께합니다. 수확한 지 하루가 지나지
            않은 채소와 과일, 그날 구운 빵을 문 앞까지 전합니다.
          </p>
          <HoverBtn light onClick={() => navigate('/shop')}>
            이번 주 수확물 보기 <span>→</span>
          </HoverBtn>
        </div>
      </section>

      {/* ─── 이번 주의 수확 ─── */}
      <section style={{ padding: 'clamp(40px,6vw,88px) clamp(20px,5vw,72px)' }}>
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'baseline',
            marginBottom: 40,
          }}
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            <h2
              style={{
                margin: 0,
                fontFamily: "'Noto Serif KR', serif",
                fontWeight: 300,
                fontSize: 26,
              }}
            >
              이번 주의 수확
            </h2>
            {/* 폴백 단계 안내 — 데이터가 없어 감출 필요는 없고,
                오히려 근거를 밝히는 게 신뢰를 준다 */}
            {bestSource && (
              <p
                style={{
                  margin: 0,
                  fontSize: 12,
                  letterSpacing: '0.08em',
                  color: '#75775e',
                }}
              >
                {BEST_SOURCE_LABEL[bestSource]}
              </p>
            )}
          </div>
          <span
            onClick={() => navigate('/shop')}
            style={{
              cursor: 'pointer',
              fontSize: 13,
              letterSpacing: '0.05em',
              borderBottom: '1px solid #333330',
              paddingBottom: 2,
            }}
          >
            전체 보기
          </span>
        </div>

        {/* mobile-2col: 데스크톱 4열 → 768px 이하에서 2열로 접힘 (index.css) */}
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
          {featured.length > 0
            ? featured.map((p) => (
                <ProductCard
                  key={p.id}
                  product={p}
                  onOpen={() => navigate(`/products/${p.id}`)}
                  onAdd={() => handleAddToCart(p)}
                  featured
                />
              ))
            : Array.from({ length: 4 }).map((_, i) => (
                <div key={i} style={{ background: '#fffef2', padding: 28 }}>
                  <div style={{ aspectRatio: '4/5', background: '#edeadb' }} />
                </div>
              ))}
        </div>
      </section>

      {/* ─── 정기배송 배너 ─── */}
      {/* mobile-1col: 정기배송 배너의 좌우 2단 → 모바일에서 위아래 1단으로 접힘 */}
      <section
        className="mobile-1col"
        style={{
          background: '#333326',
          color: '#e5e3d3',
          padding: 'clamp(48px,8vw,110px) clamp(20px,5vw,72px)',
          display: 'grid',
          gridTemplateColumns: '1fr 1fr',
          gap: 'clamp(32px,5vw,80px)',
          alignItems: 'center',
        }}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
          <p style={{ margin: 0, fontSize: 13, letterSpacing: '0.14em', color: '#a9a98f' }}>
            꾸러미 정기배송
          </p>
          <h2
            style={{
              margin: 0,
              fontFamily: "'Noto Serif KR', serif",
              fontWeight: 300,
              fontSize: 'clamp(26px,2.6vw,36px)',
              lineHeight: 1.5,
            }}
          >
            장 볼 필요 없이,
            <br />
            계절이 알아서 식탁을 차립니다.
          </h2>
          <p
            style={{
              margin: 0,
              fontSize: 15,
              lineHeight: 1.9,
              fontWeight: 300,
              color: '#c9c7b2',
              maxWidth: '46ch',
            }}
          >
            매주 그 시기에 가장 좋은 것들로 꾸린 제철 꾸러미. 어떤 채소가 올지 모르는 설렘과, 무엇이
            와도 좋다는 믿음을 함께 담았습니다.
          </p>
          <HoverBtn onClick={() => navigate('/shop')}>
            꾸러미 살펴보기 <span>→</span>
          </HoverBtn>
        </div>
        <div style={{ aspectRatio: '5/4', background: '#3f3f30' }} />
      </section>

      {/* ─── 생산자 인용구 ─── */}
      <section
        style={{
          padding: 'clamp(48px,7vw,100px) clamp(20px,5vw,72px)',
          borderBottom: '1px solid #dddaca',
        }}
      >
        <div
          style={{
            maxWidth: 680,
            margin: '0 auto',
            textAlign: 'center',
            display: 'flex',
            flexDirection: 'column',
            gap: 24,
            alignItems: 'center',
          }}
        >
          <p style={{ margin: 0, fontSize: 13, letterSpacing: '0.14em', color: '#75775e' }}>
            생산자
          </p>
          <h2
            style={{
              margin: 0,
              fontFamily: "'Noto Serif KR', serif",
              fontWeight: 300,
              fontSize: 'clamp(24px,2.4vw,32px)',
              lineHeight: 1.6,
            }}
          >
            "농사는 서두른다고 되는 일이 아니에요.
            <br />
            기다리는 게 일의 절반입니다."
          </h2>
          <p style={{ margin: 0, fontSize: 14, color: '#6d6c61', fontWeight: 300 }}>
            금산 · 흙내음농원 · 이정순 농부
          </p>
          <span
            onClick={() => navigate('/story')}
            style={{
              cursor: 'pointer',
              fontSize: 13,
              letterSpacing: '0.05em',
              borderBottom: '1px solid #333330',
              paddingBottom: 2,
            }}
          >
            스물세 농가의 이야기 읽기
          </span>
        </div>
      </section>
    </div>
  )
}

/* 호버 시 색상 반전되는 버튼 */
function HoverBtn({ onClick, children, light = false }) {
  const [hovered, setHovered] = useState(false)
  const border = light ? '#fffef2' : '#e5e3d3'
  const textColor = light ? '#fffef2' : '#e5e3d3'
  const hoverBg = light ? '#fffef2' : '#e5e3d3'
  const hoverText = light ? '#333330' : '#333326'

  return (
    <span
      onClick={onClick}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        cursor: 'pointer',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        border: `1px solid ${border}`,
        color: hovered ? hoverText : textColor,
        background: hovered ? hoverBg : 'transparent',
        padding: '16px 22px',
        maxWidth: 340,
        fontSize: 14,
        letterSpacing: '0.04em',
        userSelect: 'none',
      }}
    >
      {children}
    </span>
  )
}
