package com.kdt.shoppingmall.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.kdt.shoppingmall.config.TossPaymentsProperties;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

// MockRestServiceServer: 실제 네트워크 호출 없이 RestClient가 만드는 HTTP 요청/응답을 가짜로
// 흉내낸다. 토스 서버를 실제로 띄우지 않고도 confirm()의 성공/실패 분기를 검증할 수 있다.
class TossPaymentProcessorTest {

  private TossPaymentProcessor buildProcessor(MockRestServiceServer[] serverHolder) {
    RestClient.Builder builder = RestClient.builder();
    serverHolder[0] = MockRestServiceServer.bindTo(builder).build();
    return new TossPaymentProcessor(builder, new TossPaymentsProperties("test_sk_dummy"));
  }

  @Test
  void confirm_토스가_200응답시_true() {
    MockRestServiceServer[] holder = new MockRestServiceServer[1];
    TossPaymentProcessor processor = buildProcessor(holder);
    holder[0]
        .expect(requestTo("https://api.tosspayments.com/v1/payments/confirm"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess("{\"status\":\"DONE\"}", MediaType.APPLICATION_JSON));

    boolean result = processor.confirm("pk_test", "ORDER-1", 20000);

    assertThat(result).isTrue();
  }

  @Test
  void confirm_토스가_4xx응답시_false() {
    MockRestServiceServer[] holder = new MockRestServiceServer[1];
    TossPaymentProcessor processor = buildProcessor(holder);
    holder[0]
        .expect(requestTo("https://api.tosspayments.com/v1/payments/confirm"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withStatus(HttpStatus.BAD_REQUEST)
                .body("{\"code\":\"REJECT_CARD_COMPANY\"}")
                .contentType(MediaType.APPLICATION_JSON));

    boolean result = processor.confirm("pk_test", "ORDER-1", 20000);

    assertThat(result).isFalse();
  }

  @Test
  void confirm_네트워크오류시_false() {
    MockRestServiceServer[] holder = new MockRestServiceServer[1];
    TossPaymentProcessor processor = buildProcessor(holder);
    holder[0]
        .expect(requestTo("https://api.tosspayments.com/v1/payments/confirm"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            request -> {
              throw new IOException("연결 시간 초과");
            });

    boolean result = processor.confirm("pk_test", "ORDER-1", 20000);

    assertThat(result).isFalse();
  }
}
