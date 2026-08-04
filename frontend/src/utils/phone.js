// 사용자가 연락처를 입력하면 숫자 사이에 하이픈(-)을 자동으로 끼워
// "010-1111-1111" 형태로 만들어 주는 포맷 함수.
// 순수 함수(입력→출력만 있고 부작용 없음)라 단위 테스트로 계약을 못박기 쉽다.

// 입력값에서 숫자만 남긴 뒤 한국 전화번호 형식에 맞게 하이픈을 넣는다.
// - replace(/\D/g, ''): 숫자가 아닌 문자(하이픈·공백·문자)를 전부 제거한다.
//   덕분에 사용자가 하이픈을 지우거나 붙여넣어도 항상 숫자 기준으로 다시 포맷된다.
//
// 국번 자릿수 규칙(실제 쇼핑몰과 동일):
//   - 서울 지역번호 02 → 앞 2자리 (예: 02-1234-5678)
//   - 그 외(010 휴대폰, 031 등 지역번호, 070 인터넷전화) → 앞 3자리 (예: 010-1111-1111)
export function formatPhoneNumber(value) {
  const digits = value.replace(/\D/g, '')

  // 서울(02)은 국번이 2자리라 별도 처리한다. 총 9~10자리(02-XXX-XXXX / 02-XXXX-XXXX).
  if (digits.startsWith('02')) {
    const d = digits.slice(0, 10) // 02는 최대 10자리
    if (d.length < 3) return d // "02" 까지는 하이픈 없이
    if (d.length < 6) return `${d.slice(0, 2)}-${d.slice(2)}` // 02-XXX 입력 중
    if (d.length < 10) return `${d.slice(0, 2)}-${d.slice(2, 5)}-${d.slice(5)}` // 02-XXX-XXXX
    return `${d.slice(0, 2)}-${d.slice(2, 6)}-${d.slice(6)}` // 02-XXXX-XXXX
  }

  // 010 휴대폰 등: 3-4-4 형식, 최대 11자리.
  const d = digits.slice(0, 11)
  if (d.length < 4) return d // 3자리 이하는 하이픈 없이 그대로
  if (d.length < 8) return `${d.slice(0, 3)}-${d.slice(3)}` // 010-1111 (가운데 입력 중)
  return `${d.slice(0, 3)}-${d.slice(3, 7)}-${d.slice(7)}` // 010-1111-1111
}
