package com.kdt.shoppingmall.service;

import com.kdt.shoppingmall.config.TossPaymentsProperties;
import com.kdt.shoppingmall.domain.payment.PaymentMethod;
import com.kdt.shoppingmall.dto.payment.TossConfirmRequest;
import com.kdt.shoppingmall.dto.payment.TossConfirmResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

// 실제 토스페이먼츠 결제 승인(confirm) API를 호출하는 PaymentProcessor 구현체.
// @Component: 스프링이 이 클래스를 빈으로 등록해 OrderService에 자동 주입한다(생성자 주입).
// @EnableConfigurationProperties: TossPaymentsProperties를 "생성자 바인딩" 방식으로 빈 등록해서
// application.properties의 toss.payments.* 값을 채운 뒤 이 클래스의 생성자로 주입해준다.
@Component
@EnableConfigurationProperties(TossPaymentsProperties.class)
public class TossPaymentProcessor implements PaymentProcessor {

  private static final Logger log = LoggerFactory.getLogger(TossPaymentProcessor.class);

  private final RestClient restClient;

  public TossPaymentProcessor(RestClient.Builder builder, TossPaymentsProperties properties) {
    // 토스 인증 방식: "시크릿키:" 형태로 콜론을 붙인 뒤 BOM 없는 UTF-8로 base64 인코딩해
    // Authorization 헤더에 "Basic {인코딩값}"으로 싣는다 (토스 공식 인증 규격).
    String encoded =
        Base64.getEncoder()
            .encodeToString((properties.secretKey() + ":").getBytes(StandardCharsets.UTF_8));
    this.restClient =
        builder
            .baseUrl("https://api.tosspayments.com")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoded)
            .build();
  }

  @Override
  public ConfirmResult confirm(String paymentKey, String tossOrderId, int amount) {
    try {
      TossConfirmResponse body =
          restClient
              .post()
              .uri("/v1/payments/confirm")
              .contentType(MediaType.APPLICATION_JSON)
              .body(new TossConfirmRequest(paymentKey, tossOrderId, amount))
              .retrieve() // 2xx가 아니면 RestClientResponseException을 던진다
              .body(TossConfirmResponse.class);
      return toConfirmResult(body);
    } catch (RestClientResponseException e) {
      // 토스가 4xx로 승인을 거절한 경우(한도초과, 만료 등) — 재시도 없이 즉시 실패 처리
      log.warn("토스 결제 승인 거절: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
      return ConfirmResult.failed();
    } catch (RestClientException e) {
      // 네트워크 오류 등 — 승인 성공을 보장할 수 없으므로 안전하게 실패로 취급
      log.error("토스 결제 승인 API 호출 실패", e);
      return ConfirmResult.failed();
    }
  }

  @Override
  public ConfirmResult checkStatus(String paymentKey) {
    // 조회 자체가 실패하면(네트워크 오류, paymentKey 없음 등) 예외를 그대로 위로 던진다.
    // "아직 입금 안 됨"과 "지금 확인이 안 됨"은 의미가 다르므로 여기서 임의로 FAILED 처리하지 않는다.
    TossConfirmResponse body =
        restClient
            .get()
            .uri("/v1/payments/{paymentKey}", paymentKey)
            .retrieve()
            .body(TossConfirmResponse.class);
    return toConfirmResult(body);
  }

  // 토스 응답 바디의 문자열 status/method를 우리 도메인 타입(ConfirmStatus/PaymentMethod)으로 바꾼다.
  private ConfirmResult toConfirmResult(TossConfirmResponse body) {
    ConfirmStatus status =
        switch (body.status()) {
          case "DONE" -> ConfirmStatus.DONE;
          case "WAITING_FOR_DEPOSIT" -> ConfirmStatus.WAITING_FOR_DEPOSIT;
          default -> ConfirmStatus.FAILED;
        };
    PaymentMethod method =
        "가상계좌".equals(body.method()) ? PaymentMethod.VIRTUAL_ACCOUNT : PaymentMethod.CARD;
    TossConfirmResponse.VirtualAccount va = body.virtualAccount();
    return new ConfirmResult(
        status,
        method,
        va != null ? va.bankCode() : null,
        va != null ? va.accountNumber() : null,
        va != null ? va.dueDate() : null);
  }
}
