package com.kdt.shoppingmall.dto.address;

import jakarta.validation.constraints.NotBlank;

// 배송지 추가·수정 요청 DTO. Java record는 불변 데이터 전달에 적합해서 DTO에 주로 사용한다.
public record AddressRequest(
    @NotBlank(message = "받는 분 이름을 입력해주세요") String recipientName,
    @NotBlank(message = "연락처를 입력해주세요") String phone,
    @NotBlank(message = "우편번호를 입력해주세요") String zipCode,
    @NotBlank(message = "주소를 입력해주세요") String address,
    String addressDetail,
    String note,
    boolean isDefault) {}
