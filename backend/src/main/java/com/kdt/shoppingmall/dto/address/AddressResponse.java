package com.kdt.shoppingmall.dto.address;

import com.kdt.shoppingmall.domain.address.Address;

// 배송지 응답 DTO. 엔티티를 직접 반환하지 않고 DTO로 변환해서 반환하는 이유:
// 엔티티에는 내부 구현 세부사항(연관 엔티티 참조 등)이 있어서 직접 노출하면 의도치 않은 데이터가 직렬화될 수 있다.
public record AddressResponse(
    Long id,
    String recipientName,
    String phone,
    String zipCode,
    String address,
    String addressDetail,
    String note,
    boolean isDefault) {

  // 정적 팩토리 메서드 패턴: Address 엔티티 → AddressResponse 변환을 한 곳에서 관리한다.
  public static AddressResponse from(Address address) {
    return new AddressResponse(
        address.getId(),
        address.getRecipientName(),
        address.getPhone(),
        address.getZipCode(),
        address.getAddress(),
        address.getAddressDetail(),
        address.getNote(),
        address.isDefault());
  }
}
