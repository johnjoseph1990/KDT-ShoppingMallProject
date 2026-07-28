package com.kdt.shoppingmall.dto.payment;

import com.kdt.shoppingmall.domain.payment.Payment;
import com.kdt.shoppingmall.domain.payment.PaymentMethod;
import com.kdt.shoppingmall.domain.payment.PaymentStatus;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record PaymentResponse(
    Long orderId,
    PaymentStatus status,
    PaymentMethod paymentMethod,
    int amount,
    LocalDateTime paidAt,
    String paymentKey,
    // 가상계좌일 때만 값이 있다. 카드결제는 전부 null.
    String virtualAccountBankCode,
    String virtualAccountNumber,
    OffsetDateTime virtualAccountDueDate) {

  public static PaymentResponse from(Payment payment) {
    return new PaymentResponse(
        payment.getOrder().getId(),
        payment.getStatus(),
        payment.getPaymentMethod(),
        payment.getAmount(),
        payment.getPaidAt(),
        payment.getPaymentKey(),
        payment.getVirtualAccountBankCode(),
        payment.getVirtualAccountNumber(),
        payment.getVirtualAccountDueDate());
  }
}
