package com.kdt.shoppingmall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

// @EnableRetry: @Retryable이 붙은 메서드를 스프링이 프록시로 감싸 자동 재시도를
// 적용하도록 켜는 스위치. 이게 없으면 @Retryable을 붙여도 재시도가 동작하지 않는다.
// 이 클래스는 애플리케이션의 시작점이다. 자바에서 main 메서드는 프로그램이 실행될 때
// 제일 먼저 호출되는 메서드이므로, 스프링 부트도 여기서 웹 앱을 켠다.
@EnableRetry
@SpringBootApplication
public class ShoppingmallApplication {

  public static void main(String[] args) {
    // SpringApplication.run()는 스프링 컨테이너를 만들고, 웹 서버를 띄운 뒤
    // 애플리케이션이 요청을 받을 준비를 한다. '프로그램 실행'과 '서버 기동'을 연결하는 핵심이다.
    SpringApplication.run(ShoppingmallApplication.class, args);
  }
}
