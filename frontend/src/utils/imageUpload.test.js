// 관리자 이미지 업로드 정책의 단위 테스트.
//
// 이 테스트가 존재하는 이유:
// 2026-08-02 배포 검증에서 DEF-5(관리자 이미지 업로드 UI 부재)가 발견됐다.
// 백엔드(POST /api/admin/upload)와 Azure Blob은 정상 동작하는데 화면에 파일 선택
// UI가 없어 api/admin.js의 uploadImage()가 죽은 코드로 남아 있었다.
// UI를 붙이면서 검증 규칙을 여기로 분리했고, 규칙이 조용히 바뀌지 않도록 고정한다.
//
// 특히 MAX_IMAGE_SIZE_BYTES는 백엔드 application.properties의
// spring.servlet.multipart.max-file-size와 반드시 같아야 한다 — 그 계약을 테스트로 못 박는다.
import { describe, it, expect } from 'vitest'
import {
  MAX_IMAGE_SIZE_BYTES,
  ALLOWED_IMAGE_TYPES,
  validateImageFile,
  uploadErrorMessage,
} from './imageUpload'

// 실제 File 객체를 만들면 내용까지 있어야 해서 번거롭다.
// validateImageFile은 type과 size만 보므로, 그 두 속성만 가진 가짜 객체로 충분하다.
// (테스트 대상이 필요로 하는 최소한만 흉내내는 것을 스텁(stub)이라 한다)
const fakeFile = (type, sizeInBytes) => ({ type, size: sizeInBytes })

const MB = 1024 * 1024

describe('MAX_IMAGE_SIZE_BYTES', () => {
  it('백엔드 multipart 최대 크기(10MB)와 일치한다', () => {
    // 이 값이 백엔드보다 크면 사용자는 업로드를 다 기다린 뒤 413을 맞는다.
    // 작으면 서버가 받을 수 있는 파일을 프론트가 괜히 막는다.
    expect(MAX_IMAGE_SIZE_BYTES).toBe(10 * MB)
  })
})

describe('validateImageFile', () => {
  it('허용된 이미지 타입이고 크기도 작으면 null을 반환한다', () => {
    // null = "문제 없음". 호출부는 이 값으로 분기한다.
    expect(validateImageFile(fakeFile('image/png', 1 * MB))).toBeNull()
  })

  it('허용 목록에 있는 모든 타입을 통과시킨다', () => {
    for (const type of ALLOWED_IMAGE_TYPES) {
      expect(validateImageFile(fakeFile(type, 1 * MB))).toBeNull()
    }
  })

  it('이미지가 아닌 파일은 거부한다', () => {
    const message = validateImageFile(fakeFile('application/pdf', 1 * MB))
    expect(message).toContain('이미지 파일만')
  })

  it('type이 빈 문자열이면(브라우저가 종류를 못 알아낸 파일) 거부한다', () => {
    // 확장자 없는 파일 등에서 실제로 발생한다. 서버에 맡기지 않고 즉시 알려준다.
    expect(validateImageFile(fakeFile('', 1 * MB))).toContain('이미지 파일만')
  })

  it('10MB를 넘으면 거부하면서 한계값과 실제 크기를 함께 알려준다', () => {
    const message = validateImageFile(fakeFile('image/jpeg', 23.4 * MB))
    expect(message).toContain('10MB')
    expect(message).toContain('23.4MB') // 얼마나 줄여야 하는지 알 수 있어야 한다
  })

  it('정확히 10MB인 파일은 통과시킨다 (경계값)', () => {
    // 조건이 >= 인지 > 인지가 갈리는 지점. 백엔드도 10MB까지 허용하므로 통과가 맞다.
    expect(validateImageFile(fakeFile('image/png', MAX_IMAGE_SIZE_BYTES))).toBeNull()
  })

  it('크기가 1바이트만 초과해도 거부한다 (경계값)', () => {
    expect(validateImageFile(fakeFile('image/png', MAX_IMAGE_SIZE_BYTES + 1))).not.toBeNull()
  })

  it('이미지가 아니면서 크기도 초과하면 타입 문제를 먼저 알려준다', () => {
    // 검사 순서를 고정한다. PDF를 고른 사용자에게 "용량을 줄이세요"라고 하면
    // 엉뚱한 행동(압축)을 하게 된다 — 다른 파일을 고르라고 해야 한다.
    const message = validateImageFile(fakeFile('application/pdf', 50 * MB))
    expect(message).toContain('이미지 파일만')
  })

  it('파일이 없으면 안내 문구를 반환한다 (예외를 던지지 않는다)', () => {
    expect(validateImageFile(null)).toBe('파일을 선택해주세요.')
    expect(validateImageFile(undefined)).toBe('파일을 선택해주세요.')
  })
})

describe('uploadErrorMessage', () => {
  // axios 에러 모양을 흉내낸다: err.response.status / err.response.data.message
  const axiosError = (status, data) => ({ response: { status, data } })

  it('503이면 서버 구성 문제임을 알려준다', () => {
    // AZURE_STORAGE_CONNECTION_STRING 미주입 → AzureBlobService 빈 자체가 없는 상태.
    // 사용자가 파일을 바꿔도 해결되지 않으므로 그걸 문구로 구분해줘야 한다.
    expect(uploadErrorMessage(axiosError(503))).toContain('구성되지 않았습니다')
  })

  it('403이면 권한 문제임을 알려준다', () => {
    expect(uploadErrorMessage(axiosError(403))).toContain('권한')
  })

  it('413이면 서버가 크기 때문에 거부했음을 알려준다', () => {
    // 프론트 검증을 통과했는데 413이 났다면 프론트/백엔드 제한값이 어긋난 것이다
    expect(uploadErrorMessage(axiosError(413))).toContain('너무 커서')
  })

  it('그 외 상태코드는 서버가 보낸 message를 그대로 쓴다', () => {
    // 백엔드 GlobalExceptionHandler가 { message: "..." } 형태로 내려준다
    expect(uploadErrorMessage(axiosError(500, { message: '저장소 컨테이너가 없습니다' }))).toBe(
      '저장소 컨테이너가 없습니다',
    )
  })

  it('응답 자체가 없으면(네트워크 단절 등) 기본 문구를 쓴다', () => {
    // err.response가 undefined인 경우 — optional chaining이 없으면 여기서 터진다
    expect(uploadErrorMessage(new Error('Network Error'))).toBe('이미지 업로드에 실패했습니다.')
  })
})
