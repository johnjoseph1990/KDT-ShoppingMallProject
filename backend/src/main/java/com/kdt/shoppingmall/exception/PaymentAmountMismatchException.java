package com.kdt.shoppingmall.exception;

// 클라이언트가 보낸 결제 금액이 서버가 계산한 주문 금액과 다를 때 발생.
// 브라우저 개발자도구로 amount를 조작해도 실제 결제(토스 승인 API 호출)까지 가지 못하게 막는 방어선.
public class PaymentAmountMismatchException extends RuntimeException {

  public PaymentAmountMismatchException(String message) {
    super(message);
  }
}
