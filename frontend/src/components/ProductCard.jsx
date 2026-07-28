import { useState } from 'react'
import { fmt, stockBadge } from '../utils/product'

// ProductCard: 상품 목록(featured=false)과 홈 베스트 섹션(featured=true) 두 가지 모드를 지원한다.
// featured prop 하나로 분기해 코드 중복을 줄인 것이 이 컴포넌트의 핵심 설계다.
//
// props:
//   product  — 상품 데이터 객체 (id, name, description, price, imageUrl, stockQuantity, averageRating)
//   onOpen   — 카드 클릭 시 상세 페이지로 이동하는 콜백
//   onAdd    — 장바구니 버튼 클릭 핸들러 (featured=true이면 버튼이 없으므로 실제로 호출되지 않음)
//   featured — true면 홈 베스트 카드(배지·별점·장바구니 없음), false면 목록 카드(기본값)
export default function ProductCard({ product, onOpen, onAdd, featured = false }) {
  // JSX에서 boolean prop은 값 없이 이름만 써도 true가 된다. <ProductCard featured />는 featured={true}와 같다.
  // 기본값을 false로 두면 featured를 생략하면 목록 카드로 동작한다.
  const [hovered, setHovered] = useState(false)

  // 훅(useState 등)은 조건부로 호출하면 React "Rules of Hooks" 위반이다.
  // hovered는 항상 선언하되, soldOut/badge 계산만 featured에 따라 분기한다.
  const soldOut = !featured && product.stockQuantity === 0
  const badge = !featured ? stockBadge(product.stockQuantity) : null

  // featured 모드별 설명 글자 수 차이를 상수로 분리해 가독성을 높인다
  const descLimit = featured ? 30 : 40

  return (
    <div
      // featured=true: 카드 전체가 클릭 영역 (버튼이 없으므로 div 자체에 onClick)
      // featured=false: 이미지/텍스트 div에만 각각 onClick을 달고, 버튼은 별도로 onAdd
      onClick={featured ? onOpen : undefined}
      style={{
        background: hovered ? 'var(--color-bg-hover-light)' : 'var(--color-bg)',
        cursor: featured ? 'pointer' : 'default',
        padding: 28,
        display: 'flex',
        flexDirection: 'column',
        gap: 16,
      }}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
    >
      {/* 이미지 영역
          featured=false일 때만 position:'relative'가 필요하다.
          재고 배지(position:'absolute')의 기준점 역할을 해야 하기 때문이다.
          featured=true는 배지 자체가 없으므로 relative 설정이 불필요하다. */}
      <div
        onClick={!featured ? onOpen : undefined}
        style={{
          cursor: !featured ? 'pointer' : undefined,
          aspectRatio: '4/5',
          background: 'var(--color-bg-hover)',
          overflow: 'hidden',
          position: !featured ? 'relative' : undefined,
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
              // featured=false이고 품절일 때만 흑백+반투명 처리한다.
              // featured 카드에서는 재고 상태를 표시하지 않는 기획이므로 조건을 분리한다.
              filter: soldOut ? 'grayscale(1)' : 'none',
              opacity: soldOut ? 0.5 : 1,
            }}
          />
        )}

        {/* 재고 배지: featured=false이고 badge가 있을 때만 렌더링 */}
        {!featured && badge && (
          <span
            style={{
              position: 'absolute',
              top: 10,
              left: 10,
              background: badge.color,
              color: 'var(--color-bg)',
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

      {/* 상품 정보 텍스트 영역 */}
      <div
        onClick={!featured ? onOpen : undefined}
        style={{
          cursor: !featured ? 'pointer' : undefined,
          display: 'flex',
          flexDirection: 'column',
          gap: 6,
          // featured=false일 때 flex:1을 주는 이유:
          // 카드 높이가 달라도 장바구니 버튼이 항상 카드 아래에 고정되게 하기 위함이다.
          flex: !featured ? 1 : undefined,
        }}
      >
        {/* 상품명: 두 모드의 폰트 크기가 1px 다르다 (featured=true는 16px, false는 17px) */}
        <h3
          style={{
            margin: 0,
            fontFamily: "'Noto Serif KR', serif",
            fontWeight: 400,
            fontSize: featured ? 16 : 17,
          }}
        >
          {product.name}
        </h3>
        <p
          style={{
            margin: 0,
            fontSize: 13,
            color: 'var(--color-fg-muted)',
            fontWeight: 300,
            lineHeight: 1.7,
          }}
        >
          {product.description?.slice(0, descLimit)}
        </p>

        {/* 평균 별점: featured=false일 때만 표시.
            toFixed(1): 4.333... 같은 값을 소수점 한 자리로 반올림해 문자열로 만든다. */}
        {!featured && (
          <p
            style={{
              margin: '2px 0 0',
              fontSize: 12,
              color: 'var(--color-fg-muted)',
              fontWeight: 300,
            }}
          >
            {product.averageRating > 0 ? `★ ${product.averageRating.toFixed(1)}` : '리뷰 없음'}
          </p>
        )}

        <p style={{ margin: '6px 0 0', fontSize: 14 }}>{fmt(product.price)}</p>
      </div>

      {/* 장바구니 버튼: featured=false일 때만 렌더링
          disabled를 쓰면 키보드 탭 이동에서도 건너뛰어져 접근성 측면에서 더 정확하다.
          SVG는 stroke="currentColor"라 버튼 color를 따라가고, hover 시 함께 반전된다. */}
      {!featured && (
        <button
          onClick={onAdd}
          disabled={soldOut}
          aria-label={soldOut ? '품절된 상품' : '장바구니에 담기'}
          style={{
            cursor: soldOut ? 'not-allowed' : 'pointer',
            border: `1px solid ${soldOut ? 'var(--color-border)' : 'var(--color-fg)'}`,
            background: 'transparent',
            color: soldOut ? 'var(--color-text-disabled)' : 'var(--color-fg)',
            padding: '12px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
          onMouseEnter={(e) => {
            if (soldOut) return // 품절 버튼은 hover 반전을 하지 않는다
            e.currentTarget.style.background = 'var(--color-fg)'
            e.currentTarget.style.color = 'var(--color-bg)'
          }}
          onMouseLeave={(e) => {
            if (soldOut) return
            e.currentTarget.style.background = 'transparent'
            e.currentTarget.style.color = 'var(--color-fg)'
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
      )}
    </div>
  )
}
