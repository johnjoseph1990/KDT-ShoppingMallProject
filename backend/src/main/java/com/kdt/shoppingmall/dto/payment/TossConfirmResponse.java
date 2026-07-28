package com.kdt.shoppingmall.dto.payment;

import java.time.OffsetDateTime;

// 토스페이먼츠 결제 승인(confirm)/조회 API 응답 바디. 필드명은 토스 공식 API 명세를 그대로 따른다.
// 카드결제 응답에는 virtualAccount가 없으므로(null) 가상계좌일 때만 채워진다.
public record TossConfirmResponse(String status, String method, VirtualAccount virtualAccount) {

  // dueDate는 토스가 "2026-08-04T15:38:24+09:00"처럼 시간대 오프셋을 붙여서 내려주므로,
  // 오프셋이 없는 LocalDateTime으로는 파싱이 안 된다. OffsetDateTime을 써야 한다.
  public record VirtualAccount(String bankCode, String accountNumber, OffsetDateTime dueDate) {}
}
