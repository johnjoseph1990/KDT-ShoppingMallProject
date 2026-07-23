package com.kdt.shoppingmall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

// @EnableRetry: @Retryable이 붙은 메서드를 스프링이 프록시로 감싸 자동 재시도를
// 적용하도록 켜는 스위치. 이게 없으면 @Retryable을 붙여도 재시도가 동작하지 않는다.
@EnableRetry
@SpringBootApplication
public class ShoppingmallApplication {

  public static void main(String[] args) {
    SpringApplication.run(ShoppingmallApplication.class, args);
  }
}
