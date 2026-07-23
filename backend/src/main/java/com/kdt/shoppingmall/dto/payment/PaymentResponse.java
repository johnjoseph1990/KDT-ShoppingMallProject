package com.kdt.shoppingmall.dto.payment;

import com.kdt.shoppingmall.domain.payment.Payment;
import com.kdt.shoppingmall.domain.payment.PaymentStatus;
import java.time.LocalDateTime;

public record PaymentResponse(
    Long orderId, PaymentStatus status, int amount, LocalDateTime paidAt, String paymentKey) {

  public static PaymentResponse from(Payment payment) {
    return new PaymentResponse(
        payment.getOrder().getId(),
        payment.getStatus(),
        payment.getAmount(),
        payment.getPaidAt(),
        payment.getPaymentKey());
  }
}
