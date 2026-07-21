package com.kdt.shoppingmall.exception;

// 현재 비밀번호 확인 시 입력값이 틀렸을 때 던지는 커스텀 예외
public class PasswordMismatchException extends RuntimeException {

  public PasswordMismatchException(String message) {
    super(message);
  }
}
