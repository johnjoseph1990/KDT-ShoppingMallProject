package com.kdt.shoppingmall.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// @ConfigurationProperties: application.properties의 "toss.payments.*" 값을 이 객체 필드로
// 자동 바인딩해준다. record는 생성자가 하나뿐이라 스프링이 이걸 "생성자 바인딩" 대상으로 인식하는데,
// 이 빈은 @Component가 아니라 소비하는 쪽(TossPaymentProcessor)의 @EnableConfigurationProperties로
// 등록해야 한다. (@Component를 직접 붙이면 일반 빈 생성자 주입으로 처리돼 바인딩이 되지 않는다.)
@ConfigurationProperties(prefix = "toss.payments")
public record TossPaymentsProperties(String secretKey) {}
