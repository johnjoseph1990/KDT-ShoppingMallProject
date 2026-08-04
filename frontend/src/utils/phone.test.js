// formatPhoneNumber의 계약을 못박는 테스트.
// 입력 자릿수가 늘어날 때마다 하이픈이 올바른 위치에 들어가는지,
// 숫자가 아닌 문자는 무시되는지, 11자리를 넘기면 잘리는지를 검증한다.
import { describe, it, expect } from 'vitest'
import { formatPhoneNumber } from './phone'

describe('formatPhoneNumber', () => {
  it('3자리 이하는 하이픈 없이 그대로 둔다', () => {
    expect(formatPhoneNumber('01')).toBe('01')
    expect(formatPhoneNumber('010')).toBe('010')
  })

  it('4~7자리는 첫 하이픈만 넣는다', () => {
    expect(formatPhoneNumber('0101')).toBe('010-1')
    expect(formatPhoneNumber('0101111')).toBe('010-1111')
  })

  it('8자리 이상은 하이픈 두 개를 모두 넣는다', () => {
    expect(formatPhoneNumber('01011111')).toBe('010-1111-1')
    expect(formatPhoneNumber('01011111111')).toBe('010-1111-1111')
  })

  it('숫자가 아닌 문자(하이픈·공백·문자)는 무시하고 다시 포맷한다', () => {
    expect(formatPhoneNumber('010-1111-1111')).toBe('010-1111-1111')
    expect(formatPhoneNumber('010 1111 1111')).toBe('010-1111-1111')
    expect(formatPhoneNumber('010abc1111')).toBe('010-1111')
  })

  it('11자리를 넘는 입력은 잘라낸다', () => {
    expect(formatPhoneNumber('0101111111199')).toBe('010-1111-1111')
  })

  it('서울 지역번호(02)는 국번을 2자리로 잡는다', () => {
    expect(formatPhoneNumber('021234567')).toBe('02-123-4567') // 9자리
    expect(formatPhoneNumber('0212345678')).toBe('02-1234-5678') // 10자리
    expect(formatPhoneNumber('02-1234-5678')).toBe('02-1234-5678') // 하이픈 포함 입력
  })

  it('빈 문자열은 빈 문자열 그대로 둔다', () => {
    expect(formatPhoneNumber('')).toBe('')
  })
})
