package com.kdt.shoppingmall.exception;

/**
 * 허용되지 않은 종류의 파일을 업로드하려 할 때 던지는 예외.
 *
 * <p>RuntimeException을 상속하는 이유: 체크 예외(Exception 상속)로 만들면 호출하는 모든 메서드가 throws를 달거나 try/catch를 써야 한다.
 * 이 프로젝트의 다른 예외들(ResourceNotFoundException 등)과 마찬가지로 비즈니스 규칙 위반은 언체크 예외로 던지고
 * GlobalExceptionHandler가 한곳에서 HTTP 응답으로 변환한다.
 */
public class UnsupportedFileTypeException extends RuntimeException {

  public UnsupportedFileTypeException(String message) {
    super(message);
  }
}
