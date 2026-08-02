// 주문 상태 표기를 한 곳에서 관리하는 모듈.
//
// 왜 모듈로 뺐나: 원래 STATUS_LABEL이 AdminPage/MyPage/OrderListPage/OrderDetailPage
// 네 파일에 각각 복사돼 있었다. 가상계좌 기능을 추가할 때 OrderDetailPage 하나만
// 갱신되는 바람에, 나머지 세 화면에서 입금대기 주문이 'WAITING_FOR_DEPOSIT' 영문으로
// 노출됐다(2026-08-02 DEF-6). 같은 지식이 여러 곳에 흩어져 있으면 하나를 고칠 때
// 나머지를 잊게 되므로, 상태 표기는 여기 하나만 고치면 되도록 모았다.
//
// 백엔드 원본: backend/.../domain/order/OrderStatus.java

// 주문 상태 → 사용자에게 보여줄 한글 라벨.
// 백엔드 enum에 상태를 추가하면 여기에도 반드시 추가한다.
// (누락하면 orderStatus.test.js의 "모든 값에 한글 라벨이 있다" 테스트가 실패한다)
export const ORDER_STATUS_LABEL = {
  ORDERED: '주문완료',
  WAITING_FOR_DEPOSIT: '입금대기', // 가상계좌 발급 후 실제 입금을 기다리는 상태
  PAID: '결제완료',
  SHIPPING: '배송중',
  DELIVERED: '배송완료',
  CANCELED: '취소됨',
}

// 관리자가 주문 관리 화면에서 "수동으로 지정할 수 있는" 상태 목록.
//
// WAITING_FOR_DEPOSIT가 빠져 있는 건 실수가 아니다. 입금대기는 토스 가상계좌 발급
// 결과로 시스템이 정하는 상태지, 관리자가 임의로 만들 수 있는 상태가 아니다.
// (표시는 해야 하지만 선택은 못 하게 한다 — adminStatusOptions 참고)
export const ADMIN_SETTABLE_STATUSES = ['ORDERED', 'PAID', 'SHIPPING', 'DELIVERED', 'CANCELED']

// 관리자 주문 목록의 상태 "필터" 드롭다운에 쓸 목록.
// 필터는 조회 조건일 뿐이라 입금대기도 골라볼 수 있어야 한다.
export const ADMIN_FILTER_STATUSES = Object.keys(ORDER_STATUS_LABEL)

// 상태 코드를 한글 라벨로 바꾼다.
// 모르는 값이 오면 받은 값을 그대로 돌려준다 — 백엔드가 새 상태를 먼저 배포한
// 상황에서도 화면에 undefined가 찍히는 것보다는 낫기 때문이다.
export const orderStatusLabel = (status) => ORDER_STATUS_LABEL[status] ?? status

// 관리자 주문 행의 "상태 변경" select에 넣을 option 목록을 만든다.
//
// 핵심: 현재 상태(current)가 수동 지정 불가한 값이어도 **반드시 목록에 포함**시킨다.
// <select value={X}>인데 X와 같은 value를 가진 <option>이 없으면 브라우저가
// 첫 번째 option을 선택된 것처럼 표시한다. 실제로 이 때문에 입금대기 주문이
// 관리자 화면에서 "주문완료"로 보이는 사고가 났다(DEF-6).
// 다만 선택은 못 하게 disabled로 표시만 한다.
export const adminStatusOptions = (current) => {
  const options = ADMIN_SETTABLE_STATUSES.map((value) => ({
    value,
    label: ORDER_STATUS_LABEL[value],
    disabled: false,
  }))

  // 현재 상태가 수동 지정 가능한 목록에 이미 있으면 그대로 쓴다.
  if (!current || ADMIN_SETTABLE_STATUSES.includes(current)) return options

  // 없으면 맨 앞에 "표시 전용" option으로 끼워 넣는다.
  return [{ value: current, label: orderStatusLabel(current), disabled: true }, ...options]
}
