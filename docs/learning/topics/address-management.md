# 배송지 관리 기능 구현

> 작성 배경: 배송지 CRUD 백엔드 API + 마이페이지 탭 + 주문 화면 연동 (2026-07-22)

---

## 전체 흐름도

```
[백엔드 흐름]

GET /api/addresses/
   ↓
AddressController.getAll()
   ↓ @AuthenticationPrincipal로 memberId 추출
AddressService.getAll(memberId)
   ↓
AddressRepository.findByMemberId(memberId)
   ↓
List<AddressResponse> 반환

POST /api/addresses (배송지 추가)
   ↓
AddressController.create()
   ↓
AddressService.create(memberId, request)
   ├── isDefault=true면 기존 기본 배송지 먼저 해제
   └── Address 엔티티 저장
   ↓
AddressResponse 반환 (201 Created)

[프론트엔드 흐름]

마이페이지 → "배송지 관리" 탭 클릭
   ↓
AddressesPanel 마운트 → getAddresses() API 호출
   ↓
배송지 목록 렌더링 (기본 배송지 상단 표시)
   ├── "배송지 추가" 버튼 → AddressForm 펼침
   ├── 수정 버튼 → 해당 배송지 AddressForm 펼침(disclosure)
   └── 삭제 버튼 → deleteAddress() → 목록에서 제거

주문 화면(CartPage)
   ↓
"저장된 배송지" 버튼 클릭
   ↓
AddressPickerModal 열림 → getAddresses() API 호출
   ↓
목록에서 배송지 선택 → 폼 자동 채우기 → 모달 닫기
```

---

## 핵심 개념

### 1. `@ManyToOne` — JPA 연관관계 매핑

**초보자 설명**
"여러 배송지(Address)가 한 회원(Member)에 속한다"는 DB 관계를 Java 코드에 표현하는 방법.

**Spring/Java 어느 부분**
JPA 연관관계 어노테이션. DB에서는 `address` 테이블의 `member_id` 컬럼이 외래 키(FK)가 된다.

**실무 활용**
- 주문-회원 (`Order @ManyToOne Member`)
- 리뷰-상품 (`Review @ManyToOne Product`)
- 댓글-게시글 같은 N:1 관계 어디서든 사용

```java
// Address 엔티티
@ManyToOne
@JoinColumn(name = "member_id", nullable = false)
private Member member;
// → DB: address.member_id FK → member.id PK
```

---

### 2. Spring Data JPA 메서드 네이밍 규칙

**초보자 설명**
"메서드 이름만 맞게 지으면 SQL을 자동으로 생성해준다"

**Spring/Java 어느 부분**
`JpaRepository`를 상속한 인터페이스. Spring Data JPA가 메서드 이름을 파싱해서 JPQL/SQL로 변환.

**실무 활용**

| 메서드 이름 | 생성되는 SQL |
|-----------|------------|
| `findByMemberId(Long id)` | `WHERE member_id = ?` |
| `findByMemberIdAndIsDefaultTrue(Long id)` | `WHERE member_id = ? AND is_default = true` |
| `existsByEmail(String email)` | `SELECT COUNT(*) > 0 WHERE email = ?` |
| `deleteByMemberId(Long id)` | `DELETE WHERE member_id = ?` |

---

### 3. `@Transactional` — 기본 배송지 교체 패턴

**초보자 설명**
"기존 기본 해제 + 새 기본 설정"처럼 두 DB 작업이 반드시 함께 성공해야 할 때 묶는 도구.
중간에 예외가 나면 둘 다 롤백(없었던 일)이 된다.

**Spring/Java 어느 부분**
Spring Transaction Management. `@Service` 클래스에 `@Transactional(readOnly=true)` + 변경 메서드에 `@Transactional` 추가 패턴.

**실무 활용**
결제(포인트 차감 + 주문 생성), 이체(A 계좌 출금 + B 계좌 입금) 등 원자성이 필요한 작업.

```java
// 기본 배송지 교체 — 반드시 한 트랜잭션 안에서 처리
@Transactional
public AddressResponse create(Long memberId, AddressRequest request) {
    if (request.isDefault()) {
        clearDefaultAddresses(memberId);  // 1. 기존 기본 해제
    }
    // 2. 새 배송지 저장
    return AddressResponse.from(addressRepository.save(address));
}
```

---

### 4. OWASP A01 — 접근 제어 취약점 방어

**초보자 설명**
"내 배송지는 나만 수정/삭제할 수 있어야 한다"는 당연한 규칙을 코드로 강제하는 것.
이걸 빠뜨리면 URL에 다른 사람 배송지 ID를 넣어서 수정/삭제할 수 있다.

**Spring/Java 어느 부분**
Service 레이어의 소유권 검증 로직. Controller가 아닌 Service에서 처리하는 이유:
Controller는 HTTP 변환만 담당하고, 비즈니스 규칙(누가 접근할 수 있나)은 Service에 있어야 한다.

**실무 활용**
```java
// ❌ 잘못된 예: 배송지 ID만 확인, memberId 검증 없음
public void delete(Long addressId) {
    addressRepository.deleteById(addressId);
}

// ✅ 올바른 예: memberId도 함께 검증
private Address findAddressOwnedBy(Long memberId, Long addressId) {
    Address address = addressRepository.findById(addressId)
        .orElseThrow(() -> new ResourceNotFoundException("..."));
    if (!address.getMember().getId().equals(memberId)) {
        throw new ResourceNotFoundException("..."); // 403 대신 404로 존재 자체를 숨김
    }
    return address;
}
```

> 404로 응답하는 이유: 403(접근 거부)이면 "그 ID의 배송지가 존재함"을 알려주는 정보 노출이 된다.
> 404로 응답하면 공격자가 다른 사람의 배송지 ID를 탐지할 수 없다.

---

### 5. React `useEffect` + API 호출 패턴

**초보자 설명**
"컴포넌트가 화면에 나타나자마자 서버에서 데이터를 가져오는 표준 방법"

**React 어느 부분**
`useEffect(fn, [])` — 빈 의존성 배열이면 마운트 시 1회만 실행.

**실무 활용**
```jsx
useEffect(() => {
  getAddresses()
    .then((res) => setAddresses(res.data))
    .catch(() => {})      // 에러 시 빈 목록 유지 (화면 깨짐 방지)
    .finally(() => setLoading(false))  // 성공/실패 모두 로딩 종료
}, [])   // [] = 마운트 시 1회
```

---

## 파일 구조 (이번 작업 추가/수정 파일)

```
backend/
├── domain/address/
│   └── Address.java              ← 신규: 배송지 엔티티 (@ManyToOne Member)
├── repository/
│   └── AddressRepository.java    ← 신규: findByMemberId, findByMemberIdAndIsDefaultTrue
├── dto/address/
│   ├── AddressRequest.java       ← 신규: 추가/수정 요청 DTO (record)
│   └── AddressResponse.java      ← 신규: 응답 DTO (정적 팩토리 메서드)
├── service/
│   └── AddressService.java       ← 신규: CRUD + 소유권 검증 + 기본배송지 교체
└── controller/
    └── AddressController.java    ← 신규: GET/POST/PUT/DELETE /api/addresses

frontend/src/
├── api/
│   └── addresses.js              ← 신규: getAddresses/create/update/delete
├── pages/
│   ├── MyPage.jsx                ← 수정: "배송지 관리" 탭 추가 + AddressesPanel/AddressForm
│   └── CartPage.jsx              ← 수정: "저장된 배송지" 버튼 + AddressPickerModal
```

---

## 설계 결정: 주문과 배송지 스냅샷

Order 엔티티는 배송지를 **참조(FK)**하지 않고 **복사(flat fields)**로 저장한다:

```java
// ✅ 이 프로젝트 방식 — 주문 시점의 주소를 Order에 직접 저장
order.deliveryAddress = "서울시 강남구 테헤란로 123"

// ❌ FK 참조 방식의 문제점
// 나중에 배송지를 수정/삭제하면 과거 주문 내역의 주소도 바뀌거나 null이 됨
```

이 패턴을 "스냅샷(snapshot)"이라고 한다. 이커머스, 결제, 계약서 등 
"그 시점의 상태를 보존해야 하는" 모든 도메인에서 사용한다.

---

## 자주 하는 실수

1. **소유권 검증 누락**: `findById`만 하고 `getMember().getId()` 비교를 빠뜨리면 A01 취약점 발생.
2. **기본 배송지 중복**: `isDefault=true`로 저장 전에 `clearDefaultAddresses()`를 호출하지 않으면 기본 배송지가 둘 이상이 될 수 있다.
3. **비로그인 상태에서 저장된 배송지 버튼 클릭**: CartPage는 `PrivateRoute`로 보호되어 있지만, 비로그인 사용자가 직접 URL을 입력하면 `getAddresses()` API가 401을 반환한다 → `.catch(() => {})`로 처리.
4. **React Fragment 누락**: 하나의 `return`에서 `<form>`과 `<Modal>` 두 요소를 나란히 반환할 때 `<>...</>` Fragment가 없으면 JSX 파싱 에러.
