package com.kdt.shoppingmall.exception;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException e) {
    // Spring Security 자체가 던지는 기본 영문 메시지("Access Denied", "Access is denied")는
    // 클라이언트에 노출하지 않고 한국어로 교체한다.
    // 서비스 레이어에서 직접 던진 한국어 메시지(예: "구매자만 리뷰를 작성할 수 있습니다.")는 그대로 전달한다.
    String msg = e.getMessage();
    if (msg == null
        || msg.equalsIgnoreCase("Access Denied")
        || msg.equalsIgnoreCase("Access is denied")) {
      msg = "접근 권한이 없습니다.";
    }
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", msg));
  }

  // AuthController.login()에서 authenticationManager.authenticate()가 던지는
  // BadCredentialsException(비밀번호 틀림) · UsernameNotFoundException(가입 안 된 이메일,
  // DaoAuthenticationProvider가 내부적으로 BadCredentialsException으로 감춰서 던짐)이 여기로 온다.
  // 이 핸들러가 없으면 아래 handleUnexpected(Exception.class)가 먼저 잡아 500을 내려버려서,
  // SecurityConfig의 authenticationEntryPoint(401 설정)까지 도달하지 못하는 문제가 있었다.
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<Map<String, String>> handleAuthentication(AuthenticationException e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(Map.of("message", "이메일 또는 비밀번호가 올바르지 않습니다."));
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

  @ExceptionHandler(PaymentAmountMismatchException.class)
  public ResponseEntity<Map<String, String>> handlePaymentAmountMismatch(
      PaymentAmountMismatchException e) {
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

  // [P1-4] 동시 요청에서 서비스 레이어의 중복 선검증(existsBy...)을 동시에 통과한 뒤
  // DB 유니크 제약이 충돌하면 여기서 잡아 409를 내려준다.
  // 서비스 단에서 DuplicateXxxException이 먼저 잡히는 게 정상이지만,
  // 경쟁 조건(race condition)에서만 이 경로로 온다.
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Map<String, String>> handleDataIntegrity(
      DataIntegrityViolationException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(Map.of("message", "이미 처리된 요청입니다. 잠시 후 다시 시도해주세요."));
  }

  // 이미지가 아닌 파일을 업로드하려 한 경우. 400(잘못된 요청)으로 내려주면
  // 프론트의 uploadErrorMessage()가 default 분기에서 이 message를 그대로 화면에 띄운다.
  @ExceptionHandler(UnsupportedFileTypeException.class)
  public ResponseEntity<Map<String, String>> handleUnsupportedFileType(
      UnsupportedFileTypeException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
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

  // body가 아예 없거나 JSON 문법이 틀린 경우. @Valid 검증 이전 단계에서 발생하므로 별도 처리.
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Map<String, String>> handleUnreadable(HttpMessageNotReadableException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of("message", "요청 본문을 읽을 수 없습니다. JSON 형식을 확인해주세요."));
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
