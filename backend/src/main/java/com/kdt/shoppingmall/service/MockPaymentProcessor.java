package com.kdt.shoppingmall.service;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

// 실제 PG사 연동 전 개발용 모의 결제 처리기.
// @Component: 스프링이 빈으로 등록해서 OrderService에 자동 주입한다.
@Component
public class MockPaymentProcessor implements PaymentProcessor {

  // 90%가 성공, 10%가 실패인 모의 결제 성공률
  private static final int SUCCESS_RATE = 90;

  @Override
  public boolean isSuccess() {
    return ThreadLocalRandom.current().nextInt(100) < SUCCESS_RATE;
  }
}
