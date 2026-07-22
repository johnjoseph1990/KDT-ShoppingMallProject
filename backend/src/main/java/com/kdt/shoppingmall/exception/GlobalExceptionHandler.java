package com.kdt.shoppingmall.exception;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
  }

  @ExceptionHandler(DuplicateEmailException.class)
  public ResponseEntity<Map<String, String>> handleDuplicateEmail(DuplicateEmailException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
  }

  @ExceptionHandler(PasswordMismatchException.class)
  public ResponseEntity<Map<String, String>> handlePasswordMismatch(PasswordMismatchException e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
  }

  @ExceptionHandler(EmptyCartException.class)
  public ResponseEntity<Map<String, String>> handleEmptyCart(EmptyCartException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
  }

  @ExceptionHandler(InvalidOrderStatusException.class)
  public ResponseEntity<Map<String, String>> handleInvalidOrderStatus(
      InvalidOrderStatusException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
  }

  @ExceptionHandler(InsufficientStockException.class)
  public ResponseEntity<Map<String, String>> handleInsufficientStock(InsufficientStockException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  public ResponseEntity<Map<String, String>> handleOptimisticLock(
      OptimisticLockingFailureException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(Map.of("message", "다른 주문과 재고 처리가 충돌했습니다. 다시 시도해주세요."));
  }

  @ExceptionHandler(DuplicateReviewException.class)
  public ResponseEntity<Map<String, String>> handleDuplicateReview(DuplicateReviewException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
    String message =
        e.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .orElse("잘못된 요청입니다.");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", message));
  }

  // 위의 핸들러들이 잡지 못한 모든 예외의 최후 방어선.
  // 내부 에러(스택 트레이스 등)를 클라이언트에 노출하지 않고 서버 로그에만 기록한다.
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, String>> handleUnexpected(Exception e) {
    log.error("처리되지 않은 예외 발생", e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Map.of("message", "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
  }
}
