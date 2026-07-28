package com.kdt.shoppingmall.service;

import com.kdt.shoppingmall.domain.payment.PaymentMethod;
import java.time.OffsetDateTime;

// 결제 승인 여부를 결정하는 전략 인터페이스.
// 구현체를 교체해서 실제 PG사(토스페이먼츠) 연동 또는 테스트용 대체 구현으로 바꿀 수 있다.
public interface PaymentProcessor {

  // paymentKey/tossOrderId/amount는 결제창 SDK가 successUrl로 리다이렉트할 때 넘겨준 값이다.
  // 카드결제는 승인 즉시 DONE, 가상계좌는 발급만 되고 WAITING_FOR_DEPOSIT으로 돌아온다.
  ConfirmResult confirm(String paymentKey, String tossOrderId, int amount);

  // 가상계좌 발급 후 실제 입금이 됐는지 다시 물어볼 때 쓴다 (폴링).
  // 네트워크 오류 등으로 조회 자체가 실패하면 예외를 그대로 던진다 — "아직 입금 안 됨"과
  // "지금 확인이 안 됨"은 다른 상황이라, 호출하는 쪽(OrderService)이 구분해서 처리해야 한다.
  ConfirmResult checkStatus(String paymentKey);

  // 결제 승인/조회 결과를 하나로 표현하는 record. record는 필드값을 담기만 하는 불변 객체를
  // 짧게 선언하는 문법으로, getter/생성자/equals가 자동으로 생긴다.
  // status가 FAILED면 나머지 필드는 의미 없다(전부 null).
  record ConfirmResult(
      ConfirmStatus status,
      PaymentMethod method,
      String virtualAccountBankCode,
      String virtualAccountNumber,
      OffsetDateTime virtualAccountDueDate) {

    public static ConfirmResult failed() {
      return new ConfirmResult(ConfirmStatus.FAILED, null, null, null, null);
    }
  }

  enum ConfirmStatus {
    DONE,
    WAITING_FOR_DEPOSIT,
    FAILED
  }
}
