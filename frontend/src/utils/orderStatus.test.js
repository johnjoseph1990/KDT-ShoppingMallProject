// 주문 상태 표기 유틸의 단위 테스트.
//
// 이 테스트가 존재하는 이유(회귀 방지):
// 2026-08-02 배포 검증에서, 가상계좌 상태 WAITING_FOR_DEPOSIT가 화면에 영문 원문으로
// 노출되는 결함(DEF-6)이 발견됐다. STATUS_LABEL 상수가 4개 파일에 복사돼 있었고
// 가상계좌 기능 추가 시 OrderDetailPage 하나만 갱신됐던 게 원인이다.
// 상수를 여기로 모으고, "백엔드 enum 전체에 라벨이 있는가"를 테스트로 고정한다.
import { describe, it, expect } from 'vitest'
import {
  ORDER_STATUS_LABEL,
  ADMIN_SETTABLE_STATUSES,
  orderStatusLabel,
  adminStatusOptions,
} from './orderStatus'

// 백엔드 OrderStatus enum(domain/order/OrderStatus.java)의 전체 값.
// 백엔드에 상태를 추가하면 이 배열도 함께 늘려야 하고, 그 순간 아래 테스트가
// 라벨 누락을 잡아준다 — 이게 이 테스트의 핵심 역할이다.
const BACKEND_STATUSES = [
  'ORDERED',
  'WAITING_FOR_DEPOSIT',
  'PAID',
  'SHIPPING',
  'DELIVERED',
  'CANCELED',
]

describe('ORDER_STATUS_LABEL', () => {
  it('백엔드 OrderStatus enum의 모든 값에 한글 라벨이 있다', () => {
    // 하나라도 빠지면 화면에 영문 원문이 노출된다 (DEF-6의 증상)
    for (const status of BACKEND_STATUSES) {
      expect(ORDER_STATUS_LABEL[status], `${status} 라벨 누락`).toBeTruthy()
    }
  })

  it('라벨에 없는 상태가 정의돼 있지 않다 (오타·유령 상태 방지)', () => {
    // 반대 방향도 검사한다. 백엔드에 없는 키가 들어있으면 오타이거나
    // 삭제된 상태가 남아있는 것이다.
    expect(Object.keys(ORDER_STATUS_LABEL).sort()).toEqual([...BACKEND_STATUSES].sort())
  })

  it('가상계좌 입금대기 상태의 라벨은 "입금대기"다', () => {
    expect(ORDER_STATUS_LABEL.WAITING_FOR_DEPOSIT).toBe('입금대기')
  })
})

describe('orderStatusLabel', () => {
  it('알려진 상태는 한글 라벨로 변환한다', () => {
    expect(orderStatusLabel('WAITING_FOR_DEPOSIT')).toBe('입금대기')
    expect(orderStatusLabel('DELIVERED')).toBe('배송완료')
  })

  it('알 수 없는 상태는 받은 값을 그대로 돌려준다', () => {
    // 백엔드가 새 상태를 먼저 배포한 상황에서도 화면이 깨지지 않게 하는 안전장치.
    // 영문이 보이는 건 바람직하지 않지만, undefined가 렌더되는 것보다는 낫다.
    expect(orderStatusLabel('SOMETHING_NEW')).toBe('SOMETHING_NEW')
  })
})

describe('ADMIN_SETTABLE_STATUSES', () => {
  it('관리자가 수동으로 지정할 수 있는 상태에 WAITING_FOR_DEPOSIT는 없다', () => {
    // 입금대기는 토스 결제 흐름(가상계좌 발급)이 정하는 값이지,
    // 관리자가 임의로 만들 수 있는 상태가 아니다.
    expect(ADMIN_SETTABLE_STATUSES).not.toContain('WAITING_FOR_DEPOSIT')
  })

  it('나머지 5개 상태는 관리자가 지정할 수 있다', () => {
    expect(ADMIN_SETTABLE_STATUSES).toEqual([
      'ORDERED',
      'PAID',
      'SHIPPING',
      'DELIVERED',
      'CANCELED',
    ])
  })
})

describe('adminStatusOptions', () => {
  // DEF-6의 직접적인 재발 방지 테스트.
  // <select value={X}>인데 X와 같은 value를 가진 <option>이 없으면,
  // 브라우저가 첫 번째 option을 선택된 것처럼 표시한다 →
  // 입금대기 주문이 "주문완료"로 보이는 사고가 났다.
  it('현재 상태가 수동 지정 불가여도 옵션 목록에 반드시 포함된다', () => {
    const options = adminStatusOptions('WAITING_FOR_DEPOSIT')
    expect(options.map((o) => o.value)).toContain('WAITING_FOR_DEPOSIT')
  })

  it('수동 지정 불가한 현재 상태는 선택 못 하게 disabled 처리한다', () => {
    const options = adminStatusOptions('WAITING_FOR_DEPOSIT')
    const current = options.find((o) => o.value === 'WAITING_FOR_DEPOSIT')
    expect(current.disabled).toBe(true)
    expect(current.label).toBe('입금대기')
  })

  it('현재 상태가 수동 지정 가능하면 옵션이 늘어나지 않는다', () => {
    const options = adminStatusOptions('PAID')
    expect(options).toHaveLength(ADMIN_SETTABLE_STATUSES.length)
    expect(options.every((o) => !o.disabled)).toBe(true)
  })

  it('현재 상태를 모르는 경우에도 기본 옵션을 돌려준다', () => {
    expect(adminStatusOptions(undefined)).toHaveLength(ADMIN_SETTABLE_STATUSES.length)
  })
})
