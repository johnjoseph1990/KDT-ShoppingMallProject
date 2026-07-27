// 여러 파일에 복사-붙여넣기 돼있던 함수들을 한 곳으로 모았다 — DRY(Don't Repeat Yourself) 원칙.
// 로직이 바뀔 때 이 파일 하나만 고치면 import한 모든 파일에 즉시 반영된다.

// 숫자를 한국식 금액 문자열로 바꾼다.
// n.toLocaleString('ko-KR'): 1000 단위 콤마 삽입. 예: 15000 → "15,000원"
export function fmt(n) {
  return n.toLocaleString('ko-KR') + '원'
}

// 재고 수량을 화면 표시용 배지 객체로 바꾼다.
// "몇 개 남았을 때부터 재촉할지"는 비즈니스 판단이라 이 함수 한 곳에 모아둔다.
// 반환값: { text, color } 또는 null (배지 미표시)
export function stockBadge(stockQuantity) {
  // 검사 순서 주의: 0은 "5 이하"에도 해당하므로 품절을 반드시 먼저 걸러낸다.
  // 순서를 바꾸면 품절 상품이 '마감임박'으로 잘못 표시된다.
  if (stockQuantity === 0) return { text: '품절', color: '#e63946' }
  // 5개 이하면 재촉 배지 — 품절만큼 급하진 않으므로 빨강 대신 기본 먹색을 쓴다
  if (stockQuantity <= 5) return { text: '마감임박', color: '#333330' }
  // 재고가 넉넉하면 배지를 그리지 않는다 (null이면 렌더링 자체를 생략)
  return null
}
