// 순수 함수(pure function) 단위 테스트 — 외부 의존성이 없으므로 가장 빠르고 안정적이다.
// "단위 테스트": 함수 하나를 입력 → 출력으로만 검증. 렌더링·네트워크 없음.
import { describe, it, expect } from 'vitest'
import { fmt, stockBadge } from './product'

// describe: 연관된 테스트를 묶는 블록. 중첩도 가능하다.
describe('fmt', () => {
  // it(= test): 하나의 동작을 검증하는 블록. 한국어 메서드명 → 동작 명세 역할.
  it('숫자를 한국식 금액 문자열로 변환한다', () => {
    // expect(값).toBe(기댓값): 엄격한 동등 비교 (===). 원시값에 사용.
    expect(fmt(15000)).toBe('15,000원')
    expect(fmt(0)).toBe('0원')
  })

  it('백만 이상의 큰 금액도 천 단위 콤마가 정확히 들어간다', () => {
    expect(fmt(1000000)).toBe('1,000,000원')
  })
})

describe('stockBadge', () => {
  it('재고 0개면 품절 배지를 반환한다', () => {
    // toEqual: 객체의 내용이 같은지 비교 (toBe는 참조 비교라 객체에 부적합)
    expect(stockBadge(0)).toEqual({ text: '품절', color: '#e63946' })
  })

  it('재고 1~5개면 마감임박 배지를 반환한다', () => {
    expect(stockBadge(1)).toEqual({ text: '마감임박', color: '#333330' })
    expect(stockBadge(5)).toEqual({ text: '마감임박', color: '#333330' })
  })

  it('재고 6개 이상이면 null을 반환한다 (배지 미표시)', () => {
    // toBeNull(): null인지 확인. toBe(null)과 동일하지만 의도가 더 명확하다.
    expect(stockBadge(6)).toBeNull()
    expect(stockBadge(100)).toBeNull()
  })

  // 경계값 테스트(boundary value): 5/6 경계를 명시적으로 검증한다.
  // 이 테스트가 없으면 조건식 부등호 방향 실수(< vs <=)를 발견하기 어렵다.
  it('경계값: 재고 5개는 마감임박, 6개는 배지 없음', () => {
    expect(stockBadge(5)?.text).toBe('마감임박')
    expect(stockBadge(6)).toBeNull()
  })
})
